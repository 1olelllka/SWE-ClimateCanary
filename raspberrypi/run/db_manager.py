import aiosqlite
import json 
import logging
from datetime import datetime
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

innsbruck_time = datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

class DatabaseManager:
    def __init__(self, db_path: str):
        self.db_path = db_path

    async def init_db(self):
        """Creates all tables if they don't exist."""
        async with aiosqlite.connect(self.db_path) as db:

            await db.execute("PRAGMA journal_mode=WAL;")

            await db.execute('''
                CREATE TABLE IF NOT EXISTS measurements (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp   TEXT NOT NULL,
                    temperature REAL,
                    moisture    REAL,
                    co2         REAL
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS system_limits (
                    key        TEXT PRIMARY KEY,
                    value      REAL NOT NULL,
                    updated_at TEXT NOT NULL
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS limit_violations (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    sensor_name     TEXT NOT NULL,
                    type            TEXT NOT NULL,
                    threshold_value REAL NOT NULL,
                    actual_value    REAL NOT NULL,
                    started_at      TEXT NOT NULL,
                    resolved_at     TEXT,
                    is_active       INTEGER DEFAULT 1
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS system_logs (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp    TEXT NOT NULL,
                    category     TEXT NOT NULL,
                    message      TEXT NOT NULL,
                    synced_to_web INTEGER DEFAULT 0
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS config (
                    key        TEXT PRIMARY KEY,
                    value      TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
            ''')

            await db.commit()
            logger.info("Database initialized successfully with all tables.")

# Config

    async def get_config(self, key: str) -> str | None:
        """Fetch a single config value by key."""
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(
                "SELECT value FROM config WHERE key = ?", (key,)
            ) as cursor:
                row = await cursor.fetchone()
                return row[0] if row else None

    async def set_config(self, key: str, value: str):
        """Upsert a config value."""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                """
                INSERT INTO config (key, value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    value      = excluded.value,
                    updated_at = excluded.updated_at
                """,
                (key, str(value), innsbruck_time)
            )
            await db.commit()
            logger.debug(f"[Config] {key} = {value}")

    async def get_all_config(self) -> dict:
        """Fetch all config as a dict."""
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute("SELECT key, value FROM config") as cursor:
                rows = await cursor.fetchall()
                return {row[0]: row[1] for row in rows}

    async def get_sensors(self) -> list[dict]:
        """Returns list of sensor dicts."""
        raw = await self.get_config('sensors')
        if not raw:
            return []
        return json.loads(raw)
 
    async def set_sensors(self, sensors: list[dict]):
        """Persist the full sensor list, replacing existing."""
        await self.set_config('sensors', json.dumps(sensors))
        logger.info(f"[DB] Sensors updated: {[s['name'] for s in sensors]}")

# Measurements

    async def insert_measurement(self, temp: float, moisture: float, co2: float, timestamp: str):
        """Insert sensor readings into the measurements table."""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                "INSERT INTO measurements (timestamp, temperature, moisture, co2) VALUES (?, ?, ?, ?)",
                (timestamp, temp, moisture, co2)
            )
            await db.commit()
            logger.info(f"[DB] Measurement stored at {timestamp}: temp={temp}, moisture={moisture}, co2={co2}")

# Limits

    async def set_limit(self, key: str, value: float):
        """Save or update a threshold limit from the Webapp."""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                """
                INSERT INTO system_limits (key, value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    value      = excluded.value,
                    updated_at = excluded.updated_at
                """,
                (key, value, innsbruck_time)
            )
            await db.commit()
            logger.info(f"[DB] Limit updated: {key} = {value}")

    async def get_limit(self, key: str) -> float | None:
        """Fetch a single limit value by key."""
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(
                "SELECT value FROM system_limits WHERE key = ?", (key,)
            ) as cursor:
                row = await cursor.fetchone()
                return row[0] if row else None

    async def get_all_limits(self) -> dict:
        """Fetch all current limits so the processor can check incoming data."""
        async with aiosqlite.connect(self.db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute("SELECT key, value FROM system_limits") as cursor:
                rows = await cursor.fetchall()
                return {row["key"]: row["value"] for row in rows}

# Violations, scoped per sensor station 

    async def register_violation(self, sensor_name: str, sensor_type: str, threshold: float, actual: float):
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(
                "SELECT id FROM limit_violations WHERE sensor_name=? AND type=? AND is_active=1",
                (sensor_name, sensor_type)
            ) as cursor:
                if await cursor.fetchone():
                    return  # Already active for this sensor
 
            await db.execute(
                "INSERT INTO limit_violations (sensor_name, type, threshold_value, actual_value, started_at) VALUES (?, ?, ?, ?, ?)",
                (sensor_name, sensor_type, threshold, actual, innsbruck_time)
            )
            await db.commit()
            logger.warning(f"[DB] Violation: {sensor_name}/{sensor_type} = {actual} (limit: {threshold})")
 
    async def resolve_violation(self, sensor_name: str, sensor_type: str):
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                "UPDATE limit_violations SET is_active=0, resolved_at=? WHERE sensor_name=? AND type=? AND is_active=1",
                (innsbruck_time, sensor_name, sensor_type)
            )
            await db.commit()
            logger.info(f"[DB] Violation resolved: {sensor_name}/{sensor_type}")
 
    async def get_active_violations(self, sensor_name: str) -> list:
        async with aiosqlite.connect(self.db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM limit_violations WHERE sensor_name=? AND is_active=1",
                (sensor_name,)
            ) as cursor:
                rows = await cursor.fetchall()
                return [dict(row) for row in rows]

# Logging

    async def log_event(self, category: str, message: str, level: str = "INFO"):
        """Log to file. Persist WARN/ERROR events in DB for offline sync to Webapp."""
        if level == "ERROR":
            logger.error(f"[{category}] {message}")
        elif level in ("WARN", "WARNING"):
            logger.warning(f"[{category}] {message}")
        else:
            logger.info(f"[{category}] {message}")

        if level in ("ERROR", "WARN", "WARNING"):
            async with aiosqlite.connect(self.db_path) as db:
                await db.execute(
                    "INSERT INTO system_logs (timestamp, category, message) VALUES (?, ?, ?)",
                    (innsbruck_time, category, message)
                )
                await db.commit()

    async def get_unsynced_logs(self) -> list:
        """Fetch all logs not yet pushed to the Webapp."""
        async with aiosqlite.connect(self.db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM system_logs WHERE synced_to_web = 0 ORDER BY id ASC"
            ) as cursor:
                rows = await cursor.fetchall()
                return [dict(row) for row in rows]

    async def mark_log_synced(self, log_id: int):
        """Mark a log entry as successfully synced so it isn't sent again."""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute("UPDATE system_logs SET synced_to_web = 1 WHERE id = ?", (log_id,))
            await db.commit()
