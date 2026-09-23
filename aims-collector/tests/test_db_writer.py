"""실제 PostgreSQL 인스턴스를 대상으로 하는 통합 테스트.

infra/docker-compose.yml의 postgres 서비스(aims-core가 V1+V2 Flyway migration을
이미 적용해둔 상태)에 접속 가능해야 함. DB에 접속할 수 없으면(예: Docker Desktop을
쓸 수 없는 이 환경) 자동으로 skip됨.
"""
import uuid

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
    # 매번 새 id를 써야 로컬처럼 DB가 계속 남아있는 환경에서 이전 실행의
    # 잔여 데이터와 충돌하지 않음
    row = make_row(f"integration-test-{uuid.uuid4()}")

    first = writer.insert_batch([row])
    second = writer.insert_batch([row])

    assert first == 1
    assert second == 0  # ON CONFLICT DO NOTHING이 중복 건을 스킵함
