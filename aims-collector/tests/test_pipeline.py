import json

from aims_collector.dead_letter import DeadLetterWriter
from aims_collector.main import run_once
from aims_collector.offset_store import OffsetStore
from aims_collector.tailer import LogTailer


class FakeDbWriter:
    """실제 PostgreSQL 연결 없이도 read -> insert -> offset 순서를 검증할 수 있도록,
    실제 DbWriter의 ON CONFLICT (trace_id) DO NOTHING 동작을 흉내냄 (Docker 불필요).
    """

    def __init__(self, fail_times: int = 0):
        self.fail_times = fail_times
        self.calls = 0
        self.seen_trace_ids = set()
        self.inserted_rows = []

    def insert_batch(self, rows):
        self.calls += 1
        if self.calls <= self.fail_times:
            raise RuntimeError("simulated db failure")
        inserted = 0
        for row in rows:
            if row.trace_id in self.seen_trace_ids:
                continue
            self.seen_trace_ids.add(row.trace_id)
            self.inserted_rows.append(row)
            inserted += 1
        return inserted


def write_log(path, lines):
    with open(path, "w", encoding="utf-8", newline="") as f:
        for line in lines:
            f.write(line + "\n")


def valid_line(trace_id):
    return json.dumps({
        "timestamp": "2026-09-18T00:00:00Z",
        "service": "aims-demo",
        "level": "INFO",
        "traceId": trace_id,
        "method": "GET",
        "path": "/api/flights/{flightNumber}",
        "status": 200,
        "latencyMs": 10,
        "scenario": "NORMAL",
    })


def test_db_failure_keeps_offset_unchanged_and_batch_is_retried(tmp_path):
    log_file = tmp_path / "access.log"
    write_log(log_file, [valid_line("trace-1"), valid_line("trace-2")])

    tailer = LogTailer(str(log_file))
    offset_store = OffsetStore(str(tmp_path / "offset.json"))
    dead_letter = DeadLetterWriter(str(tmp_path / "dead_letter.log"))
    db_writer = FakeDbWriter(fail_times=1)

    # 첫 시도: DB insert 실패 -> offset이 전진하면 안 됨
    run_once(tailer, db_writer, dead_letter, offset_store)
    assert tailer.offset == 0
    assert offset_store.load() == 0
    assert db_writer.inserted_rows == []

    # 두 번째 시도(재시도): DB insert 성공 -> offset 전진, 재시도된 배치의
    # 두 행 모두 정확히 한 번씩만 삽입됨
    run_once(tailer, db_writer, dead_letter, offset_store)
    assert tailer.offset > 0
    assert offset_store.load() == tailer.offset
    assert {row.trace_id for row in db_writer.inserted_rows} == {"trace-1", "trace-2"}
    assert len(db_writer.inserted_rows) == 2  # 재시도로 인한 중복 없음


def test_reprocessing_same_batch_does_not_duplicate_rows(tmp_path):
    log_file = tmp_path / "access.log"
    write_log(log_file, [valid_line("trace-1")])

    tailer = LogTailer(str(log_file))
    offset_store = OffsetStore(str(tmp_path / "offset.json"))
    dead_letter = DeadLetterWriter(str(tmp_path / "dead_letter.log"))
    db_writer = FakeDbWriter(fail_times=0)

    run_once(tailer, db_writer, dead_letter, offset_store)
    # 메모리상 confirm된 offset이 저장되지 않고 재시작된 최악의 경우를 시뮬레이션:
    # 디스크에 저장된 offset 파일 기준으로 다시 실행
    resumed_tailer = LogTailer(str(log_file), start_offset=offset_store.load())
    run_once(resumed_tailer, db_writer, dead_letter, offset_store)

    assert len(db_writer.inserted_rows) == 1


def test_malformed_and_invalid_lines_go_to_dead_letter_without_blocking_valid_ones(tmp_path):
    log_file = tmp_path / "access.log"
    dead_letter_file = tmp_path / "dead_letter.log"
    write_log(log_file, [
        "not valid json",
        json.dumps({"service": "aims-demo"}),  # 필수 필드 누락
        valid_line("trace-1"),
    ])

    tailer = LogTailer(str(log_file))
    offset_store = OffsetStore(str(tmp_path / "offset.json"))
    dead_letter = DeadLetterWriter(str(dead_letter_file))
    db_writer = FakeDbWriter(fail_times=0)

    run_once(tailer, db_writer, dead_letter, offset_store)

    assert len(db_writer.inserted_rows) == 1
    assert db_writer.inserted_rows[0].trace_id == "trace-1"

    dead_letter_entries = [
        json.loads(line) for line in dead_letter_file.read_text(encoding="utf-8").splitlines()
    ]
    assert len(dead_letter_entries) == 2
    assert dead_letter_entries[0]["reason"].startswith("invalid_json")
    assert dead_letter_entries[1]["reason"].startswith("missing_field")
