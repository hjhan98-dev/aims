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


def run_once(tailer: LogTailer, db_writer: DbWriter, dead_letter: DeadLetterWriter,
             offset_store: OffsetStore) -> int:
    batch = tailer.read_batch()
    if not batch.lines:
        return 0

    valid_rows = []
    for raw_line in batch.lines:
        parsed = parse_line(raw_line)
        if parsed.error:
            dead_letter.write(raw_line, parsed.error)
            continue
        if parsed.record is None:
            continue

        error = validate(parsed.record)
        if error:
            dead_letter.write(raw_line, error)
            continue

        valid_rows.append(normalize(parsed.record))

    # DB insert must succeed before the offset is allowed to advance, so a
    # DB failure leaves this whole batch (valid rows and all) to be
    # re-read and retried on the next poll. ON CONFLICT DO NOTHING (see
    # db_writer.INSERT_SQL) makes that retry safe against duplicate rows.
    try:
        inserted = db_writer.insert_batch(valid_rows)
    except Exception:
        logger.exception(
            "DB insert failed; offset will not advance, batch will be retried"
        )
        return 0

    tailer.confirm(batch.next_offset)
    offset_store.save(batch.next_offset)
    logger.info(
        "processed batch: %d lines read, %d valid, %d inserted, offset -> %d",
        len(batch.lines), len(valid_rows), inserted, batch.next_offset,
    )
    return inserted


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    offset_store = OffsetStore(config.OFFSET_FILE_PATH)
    tailer = LogTailer(config.LOG_FILE_PATH, start_offset=offset_store.load())
    db_writer = DbWriter(config.DATABASE_DSN)
    dead_letter = DeadLetterWriter(config.DEAD_LETTER_FILE_PATH)

    logger.info("aims-collector starting, tailing %s", config.LOG_FILE_PATH)
    while True:
        run_once(tailer, db_writer, dead_letter, offset_store)
        time.sleep(config.POLL_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
