from aims_collector.parser import parse_line


def test_parses_valid_json_line():
    parsed = parse_line('{"a": 1, "b": "x"}')
    assert parsed.error is None
    assert parsed.record == {"a": 1, "b": "x"}


def test_blank_line_is_skipped_without_error():
    parsed = parse_line("   \n")
    assert parsed.record is None
    assert parsed.error is None


def test_malformed_json_reports_error():
    parsed = parse_line("{not valid json")
    assert parsed.record is None
    assert parsed.error is not None
    assert parsed.error.startswith("invalid_json")
