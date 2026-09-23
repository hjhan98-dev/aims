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
    """aims-demo(Phase 2) 로그 필드명을 service_log 스키마 컬럼명으로 매핑함.

    `method`, `scenario`는 로그에는 있지만 현재 스키마에 대응 컬럼이 없어서
    여기서 의도적으로 버리고 저장하지 않음.
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
