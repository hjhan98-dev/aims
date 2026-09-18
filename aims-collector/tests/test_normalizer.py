from aims_collector.normalizer import normalize


def test_normalize_maps_fields_to_service_log_schema():
    record = {
        "timestamp": "2026-09-18T00:00:00Z",
        "service": "aims-demo",
        "level": "ERROR",
        "traceId": "abc-123",
        "method": "POST",
        "path": "/api/baggage",
        "status": 500,
        "latencyMs": 3,
        "scenario": "EXCEPTION",
        "errorType": "SimulatedFailureException",
        "errorMessage": "Simulated downstream exception",
    }

    row = normalize(record)

    assert row.timestamp == "2026-09-18T00:00:00Z"
    assert row.service_name == "aims-demo"
    assert row.trace_id == "abc-123"
    assert row.log_level == "ERROR"
    assert row.endpoint == "/api/baggage"
    assert row.status_code == 500
    assert row.response_time_ms == 3
    assert row.exception_type == "SimulatedFailureException"
    assert row.exception_message == "Simulated downstream exception"


def test_normalize_defaults_missing_error_fields_to_none():
    record = {
        "timestamp": "2026-09-18T00:00:00Z",
        "service": "aims-demo",
        "level": "INFO",
        "traceId": "abc-124",
        "method": "GET",
        "path": "/api/flights/{flightNumber}",
        "status": 200,
        "latencyMs": 10,
        "scenario": "NORMAL",
    }

    row = normalize(record)

    assert row.exception_type is None
    assert row.exception_message is None
