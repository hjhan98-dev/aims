-- AIMS Phase 1 baseline schema
-- Tables: service_log, metric_snapshot, incident, incident_signal, ai_analysis

CREATE TABLE service_log (
    id                  BIGSERIAL PRIMARY KEY,
    timestamp           TIMESTAMPTZ NOT NULL,
    service_name        VARCHAR(100) NOT NULL,
    trace_id            VARCHAR(64),
    log_level           VARCHAR(20) NOT NULL,
    endpoint            VARCHAR(255),
    status_code         INTEGER,
    response_time_ms    INTEGER,
    exception_type      VARCHAR(255),
    exception_message   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_log_service_endpoint_time
    ON service_log (service_name, endpoint, timestamp);
CREATE INDEX idx_service_log_trace_id
    ON service_log (trace_id);

CREATE TABLE metric_snapshot (
    id                  BIGSERIAL PRIMARY KEY,
    service_name        VARCHAR(100) NOT NULL,
    endpoint            VARCHAR(255) NOT NULL,
    window_start        TIMESTAMPTZ NOT NULL,
    window_end          TIMESTAMPTZ NOT NULL,
    request_count       INTEGER NOT NULL,
    error_count         INTEGER NOT NULL,
    avg_latency_ms      NUMERIC(10, 2),
    p95_latency_ms      NUMERIC(10, 2),
    p99_latency_ms      NUMERIC(10, 2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_metric_snapshot_window
    ON metric_snapshot (service_name, endpoint, window_start);

CREATE TABLE incident (
    id                  BIGSERIAL PRIMARY KEY,
    service_name        VARCHAR(100) NOT NULL,
    endpoint            VARCHAR(255) NOT NULL,
    severity            VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    detected_at         TIMESTAMPTZ NOT NULL,
    resolved_at         TIMESTAMPTZ,
    summary             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_service_endpoint_status
    ON incident (service_name, endpoint, status);

CREATE TABLE incident_signal (
    id                  BIGSERIAL PRIMARY KEY,
    incident_id         BIGINT NOT NULL REFERENCES incident (id),
    signal_type         VARCHAR(50) NOT NULL,
    signal_value        NUMERIC(12, 4),
    threshold           NUMERIC(12, 4),
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_signal_incident_id
    ON incident_signal (incident_id);

CREATE TABLE ai_analysis (
    id                  BIGSERIAL PRIMARY KEY,
    incident_id         BIGINT NOT NULL REFERENCES incident (id),
    model               VARCHAR(100) NOT NULL,
    prompt_version      VARCHAR(20) NOT NULL,
    analysis            TEXT,
    suspected_cause     TEXT,
    recommendation      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_analysis_incident_id
    ON ai_analysis (incident_id);
