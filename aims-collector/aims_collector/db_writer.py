from typing import List

import psycopg2
from psycopg2.extras import execute_values

from .normalizer import ServiceLogRow

# ON CONFLICT DO NOTHING은 V2 migration에서 건 UNIQUE(trace_id) 제약이 있어야 동작함.
# "1 request = 1 access log" 전제로 trace_id를 idempotency key로 쓰는 것이므로,
# 그 전제와 향후 확장 시 주의사항은 V2 migration 파일의 주석 참고.
INSERT_SQL = """
    INSERT INTO service_log (
        timestamp, service_name, trace_id, log_level, endpoint,
        status_code, response_time_ms, exception_type, exception_message
    ) VALUES %s
    ON CONFLICT (trace_id) DO NOTHING
"""


class DbWriter:
    def __init__(self, dsn: str):
        self.dsn = dsn

    def insert_batch(self, rows: List[ServiceLogRow]) -> int:
        if not rows:
            return 0

        values = [
            (
                r.timestamp,
                r.service_name,
                r.trace_id,
                r.log_level,
                r.endpoint,
                r.status_code,
                r.response_time_ms,
                r.exception_type,
                r.exception_message,
            )
            for r in rows
        ]

        conn = psycopg2.connect(self.dsn)
        try:
            with conn:
                with conn.cursor() as cur:
                    execute_values(cur, INSERT_SQL, values)
                    # cur.rowcount는 실제로 삽입된 행 수를 반영함 - ON CONFLICT DO NOTHING으로
                    # 중복 trace_id가 스킵된 경우엔 len(rows)보다 작게 나옴 (시도 건수가 아니라 실제 삽입 건수)
                    return cur.rowcount
        finally:
            conn.close()
