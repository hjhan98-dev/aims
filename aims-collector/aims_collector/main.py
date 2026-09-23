import logging
import time

from . import config
from .dead_letter import DeadLetterWriter
from .db_writer import DbWriter
from .normalizer import normalize
from .offset_store import OffsetStore
from .parser import parse_line
from .tailer import LogTailer
from .validator import validate

logger = logging.getLogger(__name__)

# 오프셋 처리순서 - 시작점
def run_once(tailer: LogTailer, db_writer: DbWriter, dead_letter: DeadLetterWriter,
             offset_store: OffsetStore) -> int:
    batch = tailer.read_batch() # 새로 읽을 완전한 줄들을 가져옴 (offset 아직 안바뀐 상태)
    if not batch.lines:
        return 0

    valid_rows = []
    for raw_line in batch.lines:
        parsed = parse_line(raw_line)   # parse
        if parsed.error:
            dead_letter.write(raw_line, parsed.error)
            continue
        if parsed.record is None:
            continue

        error = validate(parsed.record) # validat
        if error:
            dead_letter.write(raw_line, error)
            continue

        valid_rows.append(normalize(parsed.record)) # nomalize

    # DB insert가 성공해야만 offset이 전진할 수 있음 - 그래야 DB 실패 시 이
    # 배치 전체(유효한 행 포함)가 다음 폴링에서 다시 읽혀 재시도됨.
    # ON CONFLICT DO NOTHING(db_writer.INSERT_SQL 참고)이 있어서 그 재시도가
    # 중복 행 없이 안전함.
    try:
        inserted = db_writer.insert_batch(valid_rows) # db_writer.insert_batch(valid_rows) 시도
    except Exception:
        # DB Insert에서 예외가 나면 except로 잡아서 로그만 남기고 return 0으로 끝내기. 이 시점에서는 trailer.confirm(), offset_store.save()도 호출안하고 함수 끝
        logger.exception(
            "DB insert failed; offset will not advance, batch will be retried"
        )
        return 0

    # DB 성공했을때만 도달
    tailer.confirm(batch.next_offset) # 메모리 상 offset 전진시키고
    offset_store.save(batch.next_offset) # 파일 .collector_offset에도 저장
    logger.info(
        "processed batch: %d lines read, %d valid, %d inserted, offset -> %d",
        len(batch.lines), len(valid_rows), inserted, batch.next_offset,
    )
    return inserted

# 여기가 맨 처음
def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    offset_store = OffsetStore(config.OFFSET_FILE_PATH)
    tailer = LogTailer(config.LOG_FILE_PATH, start_offset=offset_store.load())
    db_writer = DbWriter(config.DATABASE_DSN)
    dead_letter = DeadLetterWriter(config.DEAD_LETTER_FILE_PATH)

    logger.info("aims-collector starting, tailing %s", config.LOG_FILE_PATH)
    while True: # 2초마다 다음 폴링에서 run_once() 다시 호출되면 trailer.offset이 동일한 경우 if not batch.lines: 이 부분 통해서 동일배치 재시도 함
        run_once(tailer, db_writer, dead_letter, offset_store)
        time.sleep(config.POLL_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
