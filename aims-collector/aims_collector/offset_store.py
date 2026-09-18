import json
import os


class OffsetStore:
    def __init__(self, path: str):
        self.path = path

    def load(self) -> int:
        if not os.path.exists(self.path):
            return 0
        with open(self.path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return int(data.get("offset", 0))

    def save(self, offset: int) -> None:
        with open(self.path, "w", encoding="utf-8") as f:
            json.dump({"offset": offset}, f)
