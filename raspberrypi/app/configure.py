#!/usr/bin/env -S uv run
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "aiosqlite>=0.20.0",
#     "pyyaml>=6.0",
# ]
# ///

import asyncio
import os
import sys
import yaml
import aiosqlite
from datetime import datetime
from zoneinfo import ZoneInfo

CONF_PATH = os.path.join(os.path.dirname(__file__), "conf.yaml")
DB_PATH = "/home/pi/data/production.sqlite"

def now() -> str:
    return datetime.now(tz=ZoneInfo("Europe/Vienna")).isoformat()

async def set_config(db: aiosqlite.Connection, key: str, value: str) -> None:
    await db.execute(
        """
        INSERT INTO config (key, value, updated_at) VALUES (?, ?, ?)
        ON CONFLICT(key) DO UPDATE SET value=excluded.value, updated_at=excluded.updated_at
        """,
        (key, value, now()),
    )

async def main() -> None:
    if not os.path.exists(CONF_PATH):
        print(f"[configure] ERROR: {CONF_PATH} not found.", file=sys.stderr)
        sys.exit(1)

    with open(CONF_PATH) as f:
        conf = yaml.safe_load(f)

    entries = {
        "server_url": conf["webapp"]["server_url"],
        "local_listen_port": str(conf["webapp"]["local_listen_port"]),
        "auth_username": conf["auth"]["username"],
        "auth_password": conf["auth"]["password"],
        "configure_done": "1",
    }

    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)

    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("PRAGMA journal_mode=WAL;")
        await db.execute("PRAGMA foreign_keys=ON;")
        await db.execute("""
            CREATE TABLE IF NOT EXISTS config (
                key TEXT PRIMARY KEY,
                value TEXT,
                updated_at TEXT NOT NULL
            )
        """)
        for key, value in entries.items():
            await set_config(db, key, value)
        await db.commit()

    for key, value in entries.items():
        display = "*" * len(value) if "password" in key else value
        print(f" {key:<20} = {display}")
    print("[configure] Done.")

if __name__ == "__main__":
    asyncio.run(main())
