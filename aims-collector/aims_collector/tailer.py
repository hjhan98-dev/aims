import os
from dataclasses import dataclass, field
from typing import List


@dataclass
class TailBatch:
    lines: List[str] = field(default_factory=list)
    next_offset: int = 0
    rotated: bool = False


class LogTailer:
    """Reads only complete (newline-terminated) lines appended to a file
    since the last confirmed offset.

    Rotation/truncation is detected with a simple heuristic appropriate for
    a 3-day MVP: if the file's current size is smaller than the last known
    offset, the file is assumed to have been rotated or truncated, and
    reading resumes from byte 0. This does not track file identity (e.g.
    inode), so it will not catch every possible rotation edge case.
    """

    def __init__(self, file_path: str, start_offset: int = 0):
        self.file_path = file_path
        self.offset = start_offset

    def read_batch(self) -> TailBatch:
        if not os.path.exists(self.file_path):
            return TailBatch(lines=[], next_offset=self.offset, rotated=False)

        size = os.path.getsize(self.file_path)
        rotated = size < self.offset
        read_from = 0 if rotated else self.offset

        with open(self.file_path, "rb") as f:
            f.seek(read_from)
            chunk = f.read()

        last_newline = chunk.rfind(b"\n")
        if last_newline == -1:
            # No complete line since read_from yet; wait for the next poll.
            return TailBatch(lines=[], next_offset=read_from, rotated=rotated)

        complete = chunk[:last_newline]
        next_offset = read_from + last_newline + 1
        lines = [
            line.decode("utf-8", errors="replace").rstrip("\r")
            for line in complete.split(b"\n")
        ]
        return TailBatch(lines=lines, next_offset=next_offset, rotated=rotated)

    def confirm(self, next_offset: int) -> None:
        self.offset = next_offset
