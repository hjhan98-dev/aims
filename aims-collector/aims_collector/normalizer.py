from dataclasses import dataclass
from typing import Optional


@dataclass
class ServiceLogRow:
    timestamp: str
    service_name: str
    trace_id: str
    log_level: str
    endpoint: str
    status_code: int
    response_time_ms: int
    exception_type: Optional[str]
    exception_message: Optional[str]


def normalize(record: dict) -> ServiceLogRow:
    """Maps aims-demo's Phase 2 log field names to the service_log schema.

    `method` and `scenario` are present in the log but have no corresponding
    service_log column in the current schema, so they are intentionally
    dropped here rather than stored.
    """
    return ServiceLogRow(
        timestamp=record["timestamp"],
        service_name=record["service"],
        trace_id=record["traceId"],
        log_level=record["level"],
        endpoint=record["path"],
        status_code=int(record["status"]),
        response_time_ms=int(record["latencyMs"]),
        exception_type=record.get("errorType"),
        exception_message=record.get("errorMessage"),
    )
