-- Incident가 어느 metric_snapshot 윈도우에서 발생했는지 컨텍스트로 보존.
-- 기존 incident row는 이 정보를 알 수 없으므로(사후 추정 금지) nullable로 둠 -
-- 이 마이그레이션 이후 생성되는 incident부터 항상 채워짐.
ALTER TABLE incident ADD COLUMN window_start TIMESTAMPTZ;
ALTER TABLE incident ADD COLUMN window_end TIMESTAMPTZ;

-- ai_analysis는 LLM 호출을 전제로 한 이름/컬럼(model, prompt_version)이었으나,
-- Phase 5는 Rule 기반 분석이라 이름이 맞지 않음. 데이터가 없는 상태(아직 아무것도
-- 쓴 적 없음)라 데이터 이관 없이 이름과 컬럼을 함께 정리함.
DROP TABLE IF EXISTS ai_analysis;

CREATE TABLE incident_analysis (
    id                  BIGSERIAL PRIMARY KEY,
    incident_id         BIGINT NOT NULL REFERENCES incident (id),
    analysis_type       VARCHAR(20) NOT NULL,
    analysis_version    VARCHAR(20) NOT NULL,
    summary             TEXT,
    evidence            TEXT,
    recommended_checks  TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_analysis_incident_id ON incident_analysis (incident_id);
