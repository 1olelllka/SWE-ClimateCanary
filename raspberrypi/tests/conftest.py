import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'app'))

import asyncio
import pytest
from unittest.mock import AsyncMock, MagicMock

SENSOR = {
    "name": "G1T4:S3M2",
    "char_uuid": "0000ffe1-0000-1000-8000-00805f9b34fb",
    "write_uuid": "0000ffe2-0000-1000-8000-00805f9b34fb",
}

@pytest.fixture
def mock_db():
    db = AsyncMock()
    db.get_sensors.return_value = [SENSOR]
    db.get_config.return_value = "60"
    db.log_event.return_value = None
    return db

@pytest.fixture
def queues():
    return {
        "processing": asyncio.Queue(),
        "inbox":      asyncio.Queue(),
        "status":     asyncio.Queue(),
    }

@pytest.fixture
async def ble_manager(mock_db, queues):   # ← async, so a loop exists during construction
    from app.ble_manager import BLEManager
    return BLEManager(
        db=mock_db,
        sensor=SENSOR,
        processing_queue=queues["processing"],
        ble_inbox=queues["inbox"],
        status_queue=queues["status"],
        scan_lock=asyncio.Lock(),
    )
