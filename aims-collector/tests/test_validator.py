from aims_collector.validator import validate

VALID_RECORD = {
    "timestamp": "2026-09-18T00:00:00Z",
    "service": "aims-demo",
    "level": "INFO",
    "traceId": "abc-123",
    "method": "GET",
    "path": "/api/flights/{flightNumber}",
    "status": 200,
    "latencyMs": 12,
    "scenario": "NORMAL",
}


def test_valid_record_passes():
    assert validate(VALID_RECORD) is None


def test_missing_required_field_is_reported():
    record = dict(VALID_RECORD)
    del record["traceId"]
    error = validate(record)
    assert error == "missing_field:traceId"


def test_wrong_type_for_status_is_reported():
    record = dict(VALID_RECORD)
    record["status"] = "200"
    assert validate(record) == "invalid_type:status"


def test_boolean_status_is_rejected_despite_bool_being_an_int_subclass():
    record = dict(VALID_RECORD)
    record["status"] = True
    assert validate(record) == "invalid_type:status"


def test_wrong_type_for_latency_is_reported():
    record = dict(VALID_RECORD)
    record["latencyMs"] = "12"
    assert validate(record) == "invalid_type:latencyMs"


def test_null_field_is_treated_as_missing():
    record = dict(VALID_RECORD)
    record["level"] = None
    assert validate(record) == "missing_field:level"
