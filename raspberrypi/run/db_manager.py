import aiosqlite 
import logging
from datetime import datetime 

logger = logging.getLogger(__name__)

class DatabaseManager:
    def __init__(self, db_path: str):   
        self.db_path = db_path 

    async def init_db(self):
        """ Creates tables if they dont exist """
        async with aiosqlite.connect(self.db_path) as db:
            
            await db.execute("PRAGMA journal_mode=WAL;")

            await db.execute('''
                CREATE TABLE IF NOT EXISTS measurements(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    temperature REAL,
                    moisture REAL,
                    co2 REAL
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS system_limits(
                    key TEXT PRIMARY KEY,
                    value REAL NOT NULL,
                    updated_at TEXT NOT NULL 
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS limit_violations(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    threshold_value REAL NOT NULL,
                    actual_value REAL NOT NULL,
                    started_at TEXT NOT NULL,
                    resolved_at TEXT, 
                    is_active INTEGER DEFAULT 1
                )
            ''')

            await db.execute('''
                CREATE TABLE IF NOT EXISTS system_logs(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    category TEXT NOT NULL,
                    message TEXT NOT NULL,
                    synced_to_web INTEGER DEFAULT 0
                )
            ''')

            await db.commit()
            logger.info("Database initialized successfully with all tables.")

    async def insert_measurement(self, temp: float, moisture: float, co2: float):
        """ Insert current measurements into the corresponding table """
        now = datetime.now().isoformat()
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                "INSERT INTO measurements (timestamp, temperature, moisture, co2) VALUES (?, ?, ?, ?)",
                (now, temp, moisture, co2)
            )
            logger.info(f"New measurements received from sensor station at time {now}: temp={temp}, moisture={moisture}, co2={co2}")
            await db.commit()

    async def set_limit(self, key: str, value: float):
        """ Saves a new threshold limit sent by the Webapp """
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                """
                INSERT INTO system_limits (key, value, updated_at) 
                VALUES (?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET 
                value=excluded.value, updated_at=excluded.updated_at
                """,
                (key, value, datetime.now().isoformat())
            )
            await db.commit()
            logger.info(f"Limit updated via Webapp: {key} = {value}")

    async def get_all_limits(self) -> dict:
        """ Fetches all current limits so the Pi can check incoming data against them """
        async with aiosqlite.connect(self.db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute("SELECT key, value FROM system_limits") as cursor:
                rows = await cursor.fetchall()
                return {row["key"]: row["value"] for row in rows}

    async def register_violation(self, sensor_type: str, threshold: float, actual: float):
        """ Logs a new violation ONLY if one isn't already active for this sensor """
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute("SELECT id FROM limit_violations WHERE type=? AND is_active=1", (sensor_type,)) as cursor:
                if await cursor.fetchone():
                    return # Violation already active

            await db.execute(
                "INSERT INTO limit_violations (type, threshold_value, actual_value, started_at) VALUES (?, ?, ?, ?)",
                (sensor_type, threshold, actual, datetime.now().isoformat())
            )
            await db.commit()
            logger.warning(f"Limit violation. {sensor_type} hit {actual} (Limit: {threshold})")

    async def resolve_violation(self, sensor_type: str):
        """ Marks an active violation as resolved when values return to normal """
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(
                "UPDATE limit_violations SET is_active=0, resolved_at=? WHERE type=? AND is_active=1",
                (datetime.now().isoformat(), sensor_type)
            )
            await db.commit()
            logger.info(f"Violation resolved for {sensor_type}.")

    async def get_active_violations(self) -> list:
        """ Fetches currently active violations to show on the Arduino display """
        async with aiosqlite.connect(self.db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute("SELECT * FROM limit_violations WHERE is_active=1") as cursor:
                rows = await cursor.fetchall()
                return [dict(row) for row in rows]

    async def log_event(self, category: str, message: str, level: str = "INFO"):
        """ Logs to file, and queues WARN/ERRORs in SQLite to sync to Webapp """
        if level == "ERROR":
            logger.error(f"[{category}] {message}")
        elif level == "WARN":
            logger.warning(f"[{category}] {message}")
        else:
            logger.info(f"[{category}] {message}")

        if level in ["ERROR", "WARN"]:
            async with aiosqlite.connect(self.db_path) as db:
                await db.execute(
                    "INSERT INTO system_logs (timestamp, category, message) VALUES (?, ?, ?)",
                    (datetime.now().isoformat(), category, message)
                )
                await db.commit()

    async def get_unsynced_logs(self) -> list:
        """ Fetches all system logs that haven't been pushed to the Webapp yet. """
        async with aiosqlite.connect(self.db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute("SELECT * FROM system_logs WHERE synced_to_web=0 ORDER BY id ASC") as cursor:
                rows = await cursor.fetchall()
                return [dict(row) for row in rows]

    async def mark_log_synced(self, log_id: int):
        """ Marks a log as successfully synced so it isn't sent again. """
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute("UPDATE system_logs SET synced_to_web=1 WHERE id=?", (log_id,))
            await db.commit()
