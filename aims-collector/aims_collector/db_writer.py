from typing import List

import psycopg2
from psycopg2.extras import execute_values

from .normalizer import ServiceLogRow

# ON CONFLICT DO NOTHING relies on the V2 migration's UNIQUE(trace_id)
# constraint. See that migration for the "1 request = 1 access log"
# idempotency-key premise this depends on.
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
            return len(rows)
        finally:
            conn.close()
