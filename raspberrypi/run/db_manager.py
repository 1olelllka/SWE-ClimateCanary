import aiosqlite
import json 
import logging
from datetime import datetime
from zoneinfo import ZoneInfo

logger = logging.getLogger(__name__)

def get_current_time():
    return datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

class DatabaseManager:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.db = None  # Persistent connection instance

    async def connect(self):
        """Establish a persistent database connection and configure PRAGMAs."""
        if self.db is None:
            self.db = await aiosqlite.connect(self.db_path)
            self.db.row_factory = aiosqlite.Row 
            await self.db.execute("PRAGMA journal_mode=WAL;")
            logger.info(f"[DB] Connected to database at {self.db_path}")

    async def close(self):
        """Safely close the persistent connection."""
        if self.db:
            await self.db.close()
            self.db = None
            logger.info("[DB] Connection closed.")

    async def init_db(self):
        """Creates all tables if they don't exist."""
        if not self.db:
            raise RuntimeError("Database not connected. Call connect() first.")

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS measurements (
                                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                  sensor_name TEXT NOT NULL,
                                  timestamp   TEXT NOT NULL,
                                  temperature REAL,
                                  moisture    REAL,
                                  co2         REAL
                                  )
                              ''')

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS system_limits (
                                  key        TEXT PRIMARY KEY,
                                  value      REAL NOT NULL,
                                  updated_at TEXT NOT NULL
                                  )
                              ''')

        await self.db.execute('''
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

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS system_logs (
                                  id             INTEGER PRIMARY KEY AUTOINCREMENT,
                                  timestamp      TEXT NOT NULL,
                                  category       TEXT NOT NULL,
                                  message        TEXT NOT NULL,
                                  synced_to_web  INTEGER DEFAULT 0
                                  )
                              ''')

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS config (
                                  key        TEXT PRIMARY KEY,
                                  value      TEXT NOT NULL,
                                  updated_at TEXT NOT NULL
                                  )
                              ''')

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS tips (
                                  sensor_key TEXT PRIMARY KEY,
                                  tip        TEXT NOT NULL,
                                  updated_at TEXT NOT NULL
                                  )
                              ''')

        await self.db.commit()
        logger.info("[DB] Database initialized successfully with all tables.")

# Config

    async def get_config(self, key: str) -> str | None:
        """Fetch a single config value by key."""
        async with self.db.execute("SELECT value FROM config WHERE key = ?", (key,)) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def set_config(self, key: str, value: str):
        """Upsert a config value."""
        await self.db.execute(
                """
                INSERT INTO config (key, value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                value      = excluded.value,
                updated_at = excluded.updated_at
                """,
                (key, str(value), get_current_time())
                )
        await self.db.commit()
        logger.debug(f"[Config] {key} = {value}")

    async def get_all_config(self) -> dict:
        """Fetch all config as a dict."""
        async with self.db.execute("SELECT key, value FROM config") as cursor:
            rows = await cursor.fetchall()
            return {row["key"]: row["value"] for row in rows}

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

    async def insert_measurement(self, sensor_name: str, temp: float, moisture: float, co2: float, timestamp: str):
        await self.db.execute(
            "INSERT INTO measurements (sensor_name, timestamp, temperature, moisture, co2) VALUES (?, ?, ?, ?, ?)",
            (sensor_name, timestamp, temp, moisture, co2)
        )
        await self.db.commit()
        logger.debug(f"[DB] Measurement stored: sensor={sensor_name}, temp={temp}, moisture={moisture}, co2={co2}, ts={timestamp}")

# Limits

    async def set_limit(self, key: str, value: float):
        """Save or update a threshold limit from the Webapp."""
        await self.db.execute(
                """
                INSERT INTO system_limits (key, value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                value      = excluded.value,
                updated_at = excluded.updated_at
                """,
                (key, value, get_current_time())
                )
        await self.db.commit()
        logger.info(f"[DB] Limit updated: {key} = {value}")

    async def get_limit(self, key: str) -> float | None:
        """Fetch a single limit value by key."""
        async with self.db.execute("SELECT value FROM system_limits WHERE key = ?", (key,)) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def get_all_limits(self) -> dict:
        """Fetch all current limits so the processor can check incoming data."""
        async with self.db.execute("SELECT key, value FROM system_limits") as cursor:
            rows = await cursor.fetchall()
            return {row["key"]: row["value"] for row in rows}

# Violations

    async def register_violation(self, sensor_name: str, sensor_type: str, threshold: float, actual: float):
        """Register a new violation atomically, preventing TOCTOU duplicates."""
        async with self.db.execute("""
                                   INSERT INTO limit_violations (sensor_name, type, threshold_value, actual_value, started_at)
                                   SELECT ?, ?, ?, ?, ?
                                   WHERE NOT EXISTS (
                                       SELECT 1 FROM limit_violations 
                                       WHERE sensor_name = ? AND type = ? AND is_active = 1
                                       )
                                   """, (sensor_name, sensor_type, threshold, actual, get_current_time(), sensor_name, sensor_type)
                                   ) as cursor:

            await self.db.commit()

            # Only log if the WHERE NOT EXISTS check passed and a new row was added
            if cursor.rowcount > 0:
                logger.warning(f"[DB] Violation: {sensor_name}/{sensor_type} = {actual} (limit: {threshold})")

    async def resolve_violation(self, sensor_name: str, sensor_type: str):
        """Mark an active violation as resolved."""
        await self.db.execute(
                "UPDATE limit_violations SET is_active=0, resolved_at=? WHERE sensor_name=? AND type=? AND is_active=1",
                (get_current_time(), sensor_name, sensor_type)
                )
        await self.db.commit()
        logger.info(f"[DB] Violation resolved: {sensor_name}/{sensor_type}")

    async def get_active_violations(self, sensor_name: str) -> list:
        """Get all active violations for a specific sensor."""
        async with self.db.execute("SELECT * FROM limit_violations WHERE sensor_name=? AND is_active=1", (sensor_name,)) as cursor:
            rows = await cursor.fetchall()
            return [dict(row) for row in rows]

# Tips

    async def get_tip(self, sensor_key: str) -> str | None:
        """Fetch the tip string for a given sensor key (e.g. 'temperature', 'co2')."""
        async with self.db.execute("SELECT tip FROM tips WHERE sensor_key = ?", (sensor_key,)) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def get_all_tips(self) -> dict:
        """Return all tips as {sensor_key: tip_string}."""
        async with self.db.execute("SELECT sensor_key, tip FROM tips") as cursor:
            rows = await cursor.fetchall()
            return {row["sensor_key"]: row["tip"] for row in rows}

    async def set_tips(self, tips: dict):
        """Upsert the full tips map. tips = {'temperature': '...', 'co2': '...', ...}"""
        for sensor_key, tip_text in tips.items():
            await self.db.execute(
                    """
                    INSERT INTO tips (sensor_key, tip, updated_at)
                    VALUES (?, ?, ?)
                    ON CONFLICT(sensor_key) DO UPDATE SET
                    tip        = excluded.tip,
                    updated_at = excluded.updated_at
                    """,
                    (sensor_key, tip_text, get_current_time())
                    )
        await self.db.commit()
        logger.info(f"[DB] Tips updated for keys: {list(tips.keys())}")

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
            await self.db.execute(
                    "INSERT INTO system_logs (timestamp, category, message) VALUES (?, ?, ?)",
                    (get_current_time(), category, message)
                    )
            await self.db.commit()

    async def get_unsynced_logs(self) -> list:
        """Fetch all logs not yet pushed to the Webapp."""
        async with self.db.execute("SELECT * FROM system_logs WHERE synced_to_web = 0 ORDER BY id ASC") as cursor:
            rows = await cursor.fetchall()
            return [dict(row) for row in rows]

    async def mark_logs_synced(self, log_ids: list[int]):
        """Mark a batch of log entries as successfully synced."""
        placeholders = ",".join("?" * len(log_ids))
        await self.db.execute(
                f"UPDATE system_logs SET synced_to_web=1 WHERE id IN ({placeholders})",
                log_ids
                )
        await self.db.commit()
