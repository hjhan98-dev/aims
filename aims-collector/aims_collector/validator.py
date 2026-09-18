from typing import Optional

REQUIRED_FIELDS = [
    "timestamp",
    "service",
    "level",
    "traceId",
    "method",
    "path",
    "status",
    "latencyMs",
    "scenario",
]


def validate(record: dict) -> Optional[str]:
    for field_name in REQUIRED_FIELDS:
        if field_name not in record or record[field_name] is None:
            return f"missing_field:{field_name}"

    if isinstance(record["status"], bool) or not isinstance(record["status"], int):
        return "invalid_type:status"

    if isinstance(record["latencyMs"], bool) or not isinstance(
        record["latencyMs"], (int, float)
    ):
        return "invalid_type:latencyMs"

    return None
