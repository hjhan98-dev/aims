from aims_collector.tailer import LogTailer


def write(path, content):
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(content)


def test_reads_only_complete_lines(tmp_path):
    log_file = tmp_path / "access.log"
    write(log_file, '{"a":1}\n{"a":2}\n{"a":3} incomplete')

    tailer = LogTailer(str(log_file))
    batch = tailer.read_batch()

    assert batch.lines == ['{"a":1}', '{"a":2}']
    assert batch.rotated is False


def test_confirm_advances_offset_and_next_read_starts_after_it(tmp_path):
    log_file = tmp_path / "access.log"
    write(log_file, '{"a":1}\n')

    tailer = LogTailer(str(log_file))
    first = tailer.read_batch()
    tailer.confirm(first.next_offset)

    write(log_file, '{"a":1}\n{"a":2}\n')
    second = tailer.read_batch()

    assert second.lines == ['{"a":2}']


def test_incomplete_line_is_picked_up_once_completed(tmp_path):
    log_file = tmp_path / "access.log"
    write(log_file, '{"a":1} incomplete')

    tailer = LogTailer(str(log_file))
    first = tailer.read_batch()
    assert first.lines == []

    write(log_file, '{"a":1} incomplete\n')
    second = tailer.read_batch()
    assert second.lines == ['{"a":1} incomplete']


def test_rotation_is_detected_when_file_shrinks(tmp_path):
    log_file = tmp_path / "access.log"
    write(log_file, '{"a":1}\n{"a":2}\n{"a":3}\n')

    tailer = LogTailer(str(log_file))
    first = tailer.read_batch()
    tailer.confirm(first.next_offset)

    # Simulate logback rotating/truncating the active file down to a
    # single new line.
    write(log_file, '{"a":new}\n')
    second = tailer.read_batch()

    assert second.rotated is True
    assert second.lines == ['{"a":new}']


def test_missing_file_returns_empty_batch(tmp_path):
    tailer = LogTailer(str(tmp_path / "does-not-exist.log"))
    batch = tailer.read_batch()
    assert batch.lines == []
    assert batch.rotated is False
