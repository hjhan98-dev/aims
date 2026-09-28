package com.aims.core.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.aims.core.detection.rule.RuleViolation;
import com.aims.core.metric.AggregatedMetric;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// AimsCoreApplicationTests와 마찬가지로 로컬에 PostgreSQL이 떠 있어야 함
@SpringBootTest
class IncidentServiceTest {

    private static final String SERVICE_NAME = "test-incident-svc";
    private static final String ENDPOINT = "/api/test";
    private static final Instant WINDOW_START = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-02-01T00:01:00Z");

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM incident_analysis WHERE incident_id IN (SELECT id FROM incident WHERE service_name = ?)",
                SERVICE_NAME);
        jdbcTemplate.update(
                "DELETE FROM incident_signal WHERE incident_id IN (SELECT id FROM incident WHERE service_name = ?)",
                SERVICE_NAME);
        jdbcTemplate.update("DELETE FROM incident WHERE service_name = ?", SERVICE_NAME);
        jdbcTemplate.update("DELETE FROM service_log WHERE service_name = ?", SERVICE_NAME);
    }

    @Test
    void noIncidentIsCreatedWhenThereAreNoViolations() {
        incidentService.recordViolations(metric(0, 0, 0, 0, 0), List.of());

        assertThat(countIncidents()).isZero();
    }

    @Test
    void newIncidentPersistsWindowContextAndCreatesExactlyOneAnalysis() {
        insertExceptionLog(WINDOW_START.plusSeconds(5));
        RuleViolation violation = new RuleViolation("ERROR_RATE", 100.0, 5.0, "Error rate 100.00% exceeded threshold 5.00%");

        incidentService.recordViolations(metric(12, 12, 50.0, 100.0, 150.0), List.of(violation));

        Map<String, Object> incident = jdbcTemplate.queryForMap(
                "SELECT window_start, window_end FROM incident WHERE service_name = ?", SERVICE_NAME);
        assertThat(((Timestamp) incident.get("window_start")).toInstant()).isEqualTo(WINDOW_START);
        assertThat(((Timestamp) incident.get("window_end")).toInstant()).isEqualTo(WINDOW_END);

        assertThat(countAnalyses()).isEqualTo(1);
        String evidence = jdbcTemplate.queryForObject(
                "SELECT evidence FROM incident_analysis WHERE incident_id IN "
                        + "(SELECT id FROM incident WHERE service_name = ?)",
                String.class, SERVICE_NAME);
        assertThat(evidence).contains("SimulatedFailureException");
        assertThat(evidence).contains("Error Rate: 100.00%");
    }

    @Test
    void repeatedViolationsReuseIncidentAndDoNotCreateDuplicateAnalysis() {
        RuleViolation first = new RuleViolation("P95_LATENCY", 3100.0, 2000.0, "P95 latency 3100ms exceeded threshold 2000ms");
        RuleViolation second = new RuleViolation("ERROR_RATE", 8.0, 5.0, "Error rate 8.00% exceeded threshold 5.00%");

        incidentService.recordViolations(metric(12, 0, 50.0, 3100.0, 3200.0), List.of(first));
        incidentService.recordViolations(metric(12, 1, 50.0, 3100.0, 3200.0), List.of(second));

        assertThat(countIncidents()).isEqualTo(1);
        assertThat(countSignals()).isEqualTo(2);
        assertThat(countAnalyses()).isEqualTo(1); // 두 번째 위반에서는 재분석하지 않음
    }

    private AggregatedMetric metric(long requestCount, long errorCount, double avgLatencyMs,
                                     double p95LatencyMs, double p99LatencyMs) {
        return new AggregatedMetric(SERVICE_NAME, ENDPOINT, requestCount, errorCount,
                avgLatencyMs, p95LatencyMs, p99LatencyMs, WINDOW_START, WINDOW_END);
    }

    private void insertExceptionLog(Instant timestamp) {
        jdbcTemplate.update(
                "INSERT INTO service_log (timestamp, service_name, trace_id, log_level, endpoint, status_code, "
                        + "response_time_ms, exception_type, exception_message) "
                        + "VALUES (?, ?, ?, 'ERROR', ?, 500, 5, 'SimulatedFailureException', 'Simulated downstream exception')",
                Timestamp.from(timestamp), SERVICE_NAME, UUID.randomUUID().toString(), ENDPOINT);
    }

    private Integer countIncidents() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM incident WHERE service_name = ?", Integer.class, SERVICE_NAME);
    }

    private Integer countSignals() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM incident_signal isg JOIN incident i ON i.id = isg.incident_id "
                        + "WHERE i.service_name = ?",
                Integer.class, SERVICE_NAME);
    }

    private Integer countAnalyses() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM incident_analysis ia JOIN incident i ON i.id = ia.incident_id "
                        + "WHERE i.service_name = ?",
                Integer.class, SERVICE_NAME);
    }
}
