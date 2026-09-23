import os
from dataclasses import dataclass, field
from typing import List


@dataclass
class TailBatch:
    lines: List[str] = field(default_factory=list)
    next_offset: int = 0
    rotated: bool = False


class LogTailer:
    """마지막으로 confirm된 offset 이후로 파일에 추가된, 완전한(개행으로 끝나는) 줄만 읽음.

    Rotation/truncation 감지는 3일 MVP에 맞는 단순한 휴리스틱을 씀: 파일의 현재
    크기가 마지막으로 알고 있던 offset보다 작아지면 rotation/truncation이 일어난
    것으로 간주하고 byte 0부터 다시 읽음. inode 같은 파일 식별자는 추적하지 않으므로
    모든 rotation 케이스를 다 잡아내지는 못함.
    """

    def __init__(self, file_path: str, start_offset: int = 0):
        self.file_path = file_path
        self.offset = start_offset

    def read_batch(self) -> TailBatch:
        if not os.path.exists(self.file_path):
            return TailBatch(lines=[], next_offset=self.offset, rotated=False)

        # 파일의 현재 크기(size)가 저장된 self.offset 보다 작으면 rotation으로 간주 (rotated = size < self.offset)하고. 읽기 시점을 0으로 리셋 ("파일이 갑자기 작아졌다 = 로그가 롤오버/교체 됐다"는 단순한 판단기준으로 함)
        size = os.path.getsize(self.file_path)
        rotated = size < self.offset 
        read_from = 0 if rotated else self.offset

        # 파일을 바이너리 모드로 열어서 read_from 지점부터 끝까지 통째로 읽음 ( 통째로 읽는 이유 : 텍스트 모드로 열면 window네서 줄바꿈 변환때문에 바이트 오프셋 계산이 어긋날 수 있기 때문)
        with open(self.file_path, "rb") as f:
            f.seek(read_from)
            chunk = f.read()

        last_newline = chunk.rfind(b"\n") # 마지막 개행 문자의 위치를 찾음. 만약 개행이 하나도 없으면(-1) — 즉 마지막 줄이 아직 안 끝났으면 — 빈 배치를 반환하고 offset도 그대로 둡니다. 다음 폴링 때 이어서 다시 확인
        if last_newline == -1:
            # read_from 이후로 아직 완전한 줄이 없음 - 다음 폴링까지 대기
            return TailBatch(lines=[], next_offset=read_from, rotated=rotated)

        # 마지막 개행 직전(compleate)까지만 완전한 데이터로 취급하고 next_offset또 딱 그 지점 (last_newline + 1) 까지만 계산. 마지막 개행 뒤에 남은 미완성 바이트는 무시
        complete = chunk[:last_newline]
        next_offset = read_from + last_newline + 1
        lines = [
            line.decode("utf-8", errors="replace").rstrip("\r")
            for line in complete.split(b"\n")
        ]
        return TailBatch(lines=lines, next_offset=next_offset, rotated=rotated)

    # main.py에서 DB insert 성공했을때만 호출. self-offset 갱신
    def confirm(self, next_offset: int) -> None:
        self.offset = next_offset
