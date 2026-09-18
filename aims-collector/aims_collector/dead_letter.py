import json
from datetime import datetime, timezone


class DeadLetterWriter:
    def __init__(self, path: str):
        self.path = path

    def write(self, raw_line: str, reason: str) -> None:
        entry = {
            "recordedAt": datetime.now(timezone.utc).isoformat(),
            "reason": reason,
            "raw": raw_line,
        }
        with open(self.path, "a", encoding="utf-8") as f:
            f.write(json.dumps(entry) + "\n")
