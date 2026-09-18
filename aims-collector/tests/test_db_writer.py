"""Integration test against a real PostgreSQL instance.

Requires infra/docker-compose.yml's postgres service (with the V1+V2
Flyway migrations already applied by aims-core) to be reachable. Skips
itself automatically when no database is available, e.g. in this
environment where Docker Desktop cannot run.
"""
import pytest

from aims_collector import config
from aims_collector.db_writer import DbWriter
from aims_collector.normalizer import ServiceLogRow

psycopg2 = pytest.importorskip("psycopg2")


def _db_available() -> bool:
    try:
        conn = psycopg2.connect(config.DATABASE_DSN, connect_timeout=2)
        conn.close()
        return True
    except Exception:
        return False


pytestmark = pytest.mark.skipif(
    not _db_available(), reason="PostgreSQL not reachable in this environment"
)


def make_row(trace_id: str) -> ServiceLogRow:
    return ServiceLogRow(
        timestamp="2026-09-18T00:00:00Z",
        service_name="aims-demo",
        trace_id=trace_id,
        log_level="INFO",
        endpoint="/api/flights/{flightNumber}",
        status_code=200,
        response_time_ms=10,
        exception_type=None,
        exception_message=None,
    )


def test_duplicate_trace_id_is_not_inserted_twice():
    writer = DbWriter(config.DATABASE_DSN)
    row = make_row("integration-test-trace-1")

    first = writer.insert_batch([row])
    second = writer.insert_batch([row])

    assert first == 1
    assert second == 0  # ON CONFLICT DO NOTHING skipped the duplicate
