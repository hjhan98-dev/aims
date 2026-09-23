package com.aims.core.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// AimsCoreApplicationTests와 마찬가지로 로컬에 PostgreSQL이 떠 있어야 함
// (infra/docker-compose.yml 또는 application-local.yml 참고)
@SpringBootTest
class MetricSnapshotRepositoryTest {

    private static final String SERVICE_NAME = "test-metric-agg";
    private static final String ENDPOINT = "/api/test";

    @Autowired
    private MetricSnapshotRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM metric_snapshot WHERE service_name = ?", SERVICE_NAME);
        jdbcTemplate.update("DELETE FROM service_log WHERE service_name = ?", SERVICE_NAME);
    }

    @Test
    void aggregatesRequestCountErrorRateAndLatencyPercentiles() {
        Instant windowStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-01-01T00:01:00Z");

        insertLog(windowStart.plusSeconds(1), 200, 100);
        insertLog(windowStart.plusSeconds(2), 200, 200);
        insertLog(windowStart.plusSeconds(3), 500, 300);
        insertLog(windowEnd.plusSeconds(5), 200, 999); // 윈도우 밖 - 집계에서 제외돼야 함

        List<AggregatedMetric> results = repository.aggregate(windowStart, windowEnd);

        assertThat(results).hasSize(1);
        AggregatedMetric metric = results.get(0);
        assertThat(metric.serviceName()).isEqualTo(SERVICE_NAME);
        assertThat(metric.endpoint()).isEqualTo(ENDPOINT);
        assertThat(metric.requestCount()).isEqualTo(3);
        assertThat(metric.errorCount()).isEqualTo(1);
        assertThat(metric.avgLatencyMs()).isEqualTo(200.0);
    }

    @Test
    void upsertIsIdempotentForTheSameWindow() {
        Instant windowStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-01-01T00:01:00Z");
        AggregatedMetric metric = new AggregatedMetric(SERVICE_NAME, ENDPOINT, 3, 1, 200.0, 300.0, 300.0);

        repository.upsert(windowStart, windowEnd, metric);
        repository.upsert(windowStart, windowEnd, metric);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM metric_snapshot WHERE service_name = ? AND endpoint = ? AND window_start = ?",
                Integer.class, SERVICE_NAME, ENDPOINT, java.sql.Timestamp.from(windowStart));

        assertThat(count).isEqualTo(1);
    }

    private void insertLog(Instant timestamp, int statusCode, int latencyMs) {
        jdbcTemplate.update(
                "INSERT INTO service_log (timestamp, service_name, trace_id, log_level, endpoint, status_code, response_time_ms) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.from(timestamp), SERVICE_NAME, UUID.randomUUID().toString(),
                statusCode >= 500 ? "ERROR" : "INFO", ENDPOINT, statusCode, latencyMs);
    }
}
