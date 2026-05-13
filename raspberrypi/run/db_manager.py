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
        if self.db is None:
            self.db = await aiosqlite.connect(self.db_path)
            self.db.row_factory = aiosqlite.Row 
            await self.db.execute("PRAGMA journal_mode=WAL;")
            logger.info(f"[DB] Connected to database at {self.db_path}")

    async def close(self):
        if self.db:
            await self.db.close()
            self.db = None
            logger.info("[DB] Connection closed.")

    async def init_db(self):
        if not self.db:
            raise RuntimeError("Database not connected. Call connect() first.")

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS measurements (
                                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                                  sensor_name TEXT NOT NULL,
                                  timestamp TEXT NOT NULL,
                                  temperature REAL,
                                  moisture REAL,
                                  co2 REAL
                                  )
                              ''')

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS system_limits (
                                  key TEXT PRIMARY KEY,
                                  value REAL NOT NULL,
                                  updated_at TEXT NOT NULL
                                  )
                              ''')

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS limit_violations (
                                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                                  sensor_name TEXT NOT NULL,
                                  type TEXT NOT NULL,
                                  threshold_value REAL NOT NULL,
                                  actual_value REAL NOT NULL,
                                  started_at TEXT NOT NULL,
                                  resolved_at TEXT,
                                  is_active INTEGER DEFAULT 1,
                                  warning_id TEXT
                                  )
                              ''')

        await self.db.execute('''
                              CREATE TABLE IF NOT EXISTS config (
                                  key TEXT PRIMARY KEY,
                                  value TEXT NOT NULL,
                                  updated_at TEXT NOT NULL
                                  )
                              ''')

        await self.db.commit()
        logger.info("[DB] Database initialized successfully with all tables.")

# Config

    async def get_config(self, key: str) -> str | None:
        async with self.db.execute("SELECT value FROM config WHERE key = ?", (key,)) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def set_config(self, key: str, value: str):
        await self.db.execute(
                """
                INSERT INTO config (key, value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                value = excluded.value,
                updated_at = excluded.updated_at
                """,
                (key, str(value), get_current_time())
                )
        await self.db.commit()
        logger.debug(f"[Config] {key} = {value}")

    async def get_all_config(self) -> dict:
        async with self.db.execute("SELECT key, value FROM config") as cursor:
            rows = await cursor.fetchall()
            return {row["key"]: row["value"] for row in rows}

    async def get_sensors(self) -> list[dict]:
        raw = await self.get_config('sensors')
        if not raw:
            return []
        return json.loads(raw)

    async def set_sensors(self, sensors: list[dict]):
        await self.set_config('sensors', json.dumps(sensors))
        logger.info(f"[DB] Sensors updated: {[s['name'] for s in sensors]}")

    async def add_sensors(self, new_sensors: list[dict]):
        current = await self.get_sensors()
        existing_names = {s['name'] for s in current}
        to_add = [s for s in new_sensors if s['name'] not in existing_names]
        if to_add:
            await self.set_sensors(current + to_add)
            logger.info(f"[DB] Added sensors: {[s['name'] for s in to_add]}")
        else:
            logger.info("[DB] No new sensors to add (all already present).")

    async def delete_sensors_by_ids(self, sensor_ids: list[str]):
        current = await self.get_sensors()
        id_set = {str(sid) for sid in sensor_ids}
        remaining = [
            s for s in current
            if str(s.get('char_uuid', '')) not in id_set
            and str(s.get('write_uuid', '')) not in id_set
        ]
        removed = len(current) - len(remaining)
        await self.set_sensors(remaining)
        logger.info(f"[DB] Deleted {removed} sensor(s) by id. Remaining: {[s['name'] for s in remaining]}")

    async def delete_all_sensors(self):
        await self.set_sensors([])
        logger.info("[DB] All sensors flushed from database.")

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
        await self.db.execute(
                """
                INSERT INTO system_limits (key, value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                value = excluded.value,
                updated_at = excluded.updated_at
                """,
                (key, value, get_current_time())
                )
        await self.db.commit()
        logger.info(f"[DB] Limit updated: {key} = {value}")

    async def get_limit(self, key: str) -> float | None:
        async with self.db.execute("SELECT value FROM system_limits WHERE key = ?", (key,)) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def get_all_limits(self) -> dict:
        async with self.db.execute("SELECT key, value FROM system_limits") as cursor:
            rows = await cursor.fetchall()
            return {row["key"]: row["value"] for row in rows}

# Violations

    async def register_violation(self, sensor_name: str, sensor_type: str, threshold: float, actual: float):
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
        await self.db.execute(
                "UPDATE limit_violations SET is_active=0, resolved_at=? WHERE sensor_name=? AND type=? AND is_active=1",
                (get_current_time(), sensor_name, sensor_type)
                )
        await self.db.commit()
        logger.info(f"[DB] Violation resolved: {sensor_name}/{sensor_type}")

    async def save_warning_id(self, sensor_name: str, sensor_type: str, warning_id: str):
        await self.db.execute(
            "UPDATE limit_violations SET warning_id=? WHERE sensor_name=? AND type=? AND is_active=1",
            (warning_id, sensor_name, sensor_type)
        )
        await self.db.commit()
        logger.debug(f"[DB] warning_id={warning_id} saved for {sensor_name}/{sensor_type}")

    async def get_warning_id(self, sensor_name: str, sensor_type: str) -> str | None:
        async with self.db.execute(
            """
            SELECT warning_id FROM limit_violations
            WHERE sensor_name=? AND type=?
            ORDER BY id DESC LIMIT 1
            """,
            (sensor_name, sensor_type)
        ) as cursor:
            row = await cursor.fetchone()
            return row[0] if row else None

    async def get_active_violations(self, sensor_name: str) -> list:
        async with self.db.execute("SELECT * FROM limit_violations WHERE sensor_name=? AND is_active=1", (sensor_name,)) as cursor:
            rows = await cursor.fetchall()
            return [dict(row) for row in rows]

# Logging

    async def log_event(self, category: str, message: str, level: str = "INFO"):
        if level == "ERROR":
            logger.error(f"[{category}] {message}")
        elif level in ("WARN", "WARNING"):
            logger.warning(f"[{category}] {message}")
        else:
            logger.info(f"[{category}] {message}")
