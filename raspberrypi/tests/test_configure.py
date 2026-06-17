import asyncio
import os
import sys
import pytest
import aiosqlite
from datetime import datetime
from unittest.mock import patch, mock_open, MagicMock, AsyncMock
from zoneinfo import ZoneInfo

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "app"))
import configure  


class TestNow:
    """now() returns a timezone-aware ISO-8601 string in Vienna local time."""

    def test_returns_string(self):
        result = configure.now()
        assert isinstance(result, str)

    def test_is_valid_isoformat(self):
        result = configure.now()
        # Should not raise
        dt = datetime.fromisoformat(result)
        assert dt.tzinfo is not None

    def test_uses_vienna_timezone(self):
        result = configure.now()
        dt = datetime.fromisoformat(result)
        vienna = ZoneInfo("Europe/Vienna")
        # UTC offset must match Vienna's current offset
        expected_offset = datetime.now(tz=vienna).utcoffset()
        assert dt.utcoffset() == expected_offset


class TestSetConfig:
    """set_config() upserts a key/value pair in the config table with an updated_at timestamp."""

    @pytest.fixture()
    async def db(self):
        async with aiosqlite.connect(":memory:") as conn:
            await conn.execute("""
                CREATE TABLE config (
                    key TEXT PRIMARY KEY,
                    value TEXT,
                    updated_at TEXT NOT NULL
                )
            """)
            await conn.commit()
            yield conn

    @pytest.mark.asyncio
    async def test_inserts_new_row(self, db):
        await configure.set_config(db, "foo", "bar")
        await db.commit()

        async with db.execute("SELECT value FROM config WHERE key='foo'") as cur:
            row = await cur.fetchone()
        assert row is not None
        assert row[0] == "bar"

    @pytest.mark.asyncio
    async def test_upserts_existing_key(self, db):
        await configure.set_config(db, "foo", "first")
        await configure.set_config(db, "foo", "second")
        await db.commit()

        async with db.execute("SELECT value FROM config WHERE key='foo'") as cur:
            rows = await cur.fetchall()
        assert len(rows) == 1
        assert rows[0][0] == "second"

    @pytest.mark.asyncio
    async def test_updated_at_is_set(self, db):
        await configure.set_config(db, "ts_key", "val")
        await db.commit()

        async with db.execute("SELECT updated_at FROM config WHERE key='ts_key'") as cur:
            row = await cur.fetchone()
        assert row is not None
        dt = datetime.fromisoformat(row[0])
        assert dt.tzinfo is not None  # timezone-aware

    @pytest.mark.asyncio
    async def test_updated_at_changes_on_update(self, db):
        fixed_times = [
            "2024-01-01T10:00:00+01:00",
            "2024-01-01T11:00:00+01:00",
        ]
        call_count = 0

        def fake_now():
            nonlocal call_count
            t = fixed_times[call_count % len(fixed_times)]
            call_count += 1
            return t

        with patch.object(configure, "now", side_effect=fake_now):
            await configure.set_config(db, "k", "v1")
            await configure.set_config(db, "k", "v2")
            await db.commit()

        async with db.execute("SELECT updated_at FROM config WHERE key='k'") as cur:
            row = await cur.fetchone()
        assert row[0] == "2024-01-01T11:00:00+01:00"

    @pytest.mark.asyncio
    async def test_multiple_keys_independent(self, db):
        await configure.set_config(db, "a", "1")
        await configure.set_config(db, "b", "2")
        await db.commit()

        async with db.execute("SELECT key, value FROM config ORDER BY key") as cur:
            rows = await cur.fetchall()
        assert dict(rows) == {"a": "1", "b": "2"}

class TestMainMissingConf:
    """main() exits with code 1 and prints an error when the YAML config file is absent."""

    @pytest.mark.asyncio
    async def test_exits_when_conf_missing(self, tmp_path, capsys):
        with patch.object(configure, "CONF_PATH", str(tmp_path / "nonexistent.yaml")):
            with pytest.raises(SystemExit) as exc_info:
                await configure.main()
        assert exc_info.value.code == 1

    @pytest.mark.asyncio
    async def test_error_message_on_stderr(self, tmp_path, capsys):
        missing = str(tmp_path / "nonexistent.yaml")
        with patch.object(configure, "CONF_PATH", missing):
            with pytest.raises(SystemExit):
                await configure.main()
        captured = capsys.readouterr()
        assert "ERROR" in captured.err
        assert missing in captured.err

VALID_CONF = {
    "webapp": {"server_url": "https://example.com", "local_listen_port": 8080},
    "auth": {"username": "admin", "password": "s3cr3t"},
}


class TestMainHappyPath:
    """main() reads the YAML config, seeds the DB, and prints a masked completion summary."""

    @pytest.fixture()
    def conf_file(self, tmp_path):
        import yaml
        p = tmp_path / "conf.yaml"
        p.write_text(yaml.dump(VALID_CONF))
        return str(p)

    @pytest.fixture()
    def db_file(self, tmp_path):
        return str(tmp_path / "data" / "test.sqlite")

    @pytest.mark.asyncio
    async def test_writes_all_keys_to_db(self, conf_file, db_file):
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()

        async with aiosqlite.connect(db_file) as db:
            async with db.execute("SELECT key, value FROM config ORDER BY key") as cur:
                rows = dict(await cur.fetchall())

        assert rows["server_url"] == "https://example.com"
        assert rows["local_listen_port"] == "8080"
        assert rows["auth_username"] == "admin"
        assert rows["auth_password"] == "s3cr3t"
        assert rows["configure_done"] == "1"

    @pytest.mark.asyncio
    async def test_password_masked_in_stdout(self, conf_file, db_file, capsys):
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()

        out = capsys.readouterr().out
        assert "s3cr3t" not in out
        assert "******" in out  # len("s3cr3t") == 6

    @pytest.mark.asyncio
    async def test_non_password_values_shown_plaintext(self, conf_file, db_file, capsys):
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()

        out = capsys.readouterr().out
        assert "https://example.com" in out
        assert "8080" in out
        assert "admin" in out

    @pytest.mark.asyncio
    async def test_done_message_printed(self, conf_file, db_file, capsys):
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()

        out = capsys.readouterr().out
        assert "[configure] Done." in out

    @pytest.mark.asyncio
    async def test_idempotent_second_run(self, conf_file, db_file):
        """Running twice must not raise and must leave correct final values."""
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()
            await configure.main()

        async with aiosqlite.connect(db_file) as db:
            async with db.execute("SELECT COUNT(*) FROM config") as cur:
                count = (await cur.fetchone())[0]
        assert count == 5  # exactly 5 keys, no duplicates

    @pytest.mark.asyncio
    async def test_creates_db_directory_if_missing(self, conf_file, tmp_path):
        db_file = str(tmp_path / "deep" / "nested" / "dir" / "test.sqlite")
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()  # must not raise
        assert os.path.exists(db_file)

    @pytest.mark.asyncio
    async def test_wal_journal_mode_enabled(self, conf_file, db_file):
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()

        async with aiosqlite.connect(db_file) as db:
            async with db.execute("PRAGMA journal_mode;") as cur:
                mode = (await cur.fetchone())[0]
        assert mode == "wal"

    @pytest.mark.asyncio
    async def test_port_stored_as_string(self, conf_file, db_file):
        with (
            patch.object(configure, "CONF_PATH", conf_file),
            patch.object(configure, "DB_PATH", db_file),
        ):
            await configure.main()

        async with aiosqlite.connect(db_file) as db:
            async with db.execute(
                "SELECT value FROM config WHERE key='local_listen_port'"
            ) as cur:
                val = (await cur.fetchone())[0]
        assert val == "8080"
        assert isinstance(val, str)
