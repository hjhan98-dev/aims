import json
from dataclasses import dataclass
from typing import Optional


@dataclass
class ParsedLine:
    record: Optional[dict] = None
    error: Optional[str] = None


def parse_line(raw_line: str) -> ParsedLine:
    stripped = raw_line.strip()
    if not stripped:
        return ParsedLine(record=None, error=None)
    try:
        return ParsedLine(record=json.loads(stripped), error=None)
    except json.JSONDecodeError as e:
        return ParsedLine(record=None, error=f"invalid_json: {e}")
