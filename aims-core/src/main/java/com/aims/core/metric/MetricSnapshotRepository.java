package com.aims.core.metric;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MetricSnapshotRepository {

    private static final String AGGREGATE_SQL = """
            SELECT service_name, endpoint,
                   count(*) AS request_count,
                   count(*) FILTER (WHERE status_code >= 500) AS error_count,
                   avg(response_time_ms) AS avg_latency_ms,
                   percentile_cont(0.95) WITHIN GROUP (ORDER BY response_time_ms) AS p95_latency_ms,
                   percentile_cont(0.99) WITHIN GROUP (ORDER BY response_time_ms) AS p99_latency_ms
            FROM service_log
            WHERE timestamp >= ? AND timestamp < ?
            GROUP BY service_name, endpoint
            """;

    // metric_snapshot의 UNIQUE(service_name, endpoint, window_start) 제약
    // (V1__init_schema.sql 참고)에 의존함 - 같은 윈도우를 다시 집계해도
    // 중복 행이 생기지 않음
    private static final String UPSERT_SQL = """
            INSERT INTO metric_snapshot (
                service_name, endpoint, window_start, window_end,
                request_count, error_count, avg_latency_ms, p95_latency_ms, p99_latency_ms
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (service_name, endpoint, window_start) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public MetricSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AggregatedMetric> aggregate(Instant windowStart, Instant windowEnd) {
        return jdbcTemplate.query(
                AGGREGATE_SQL,
                (rs, rowNum) -> new AggregatedMetric(
                        rs.getString("service_name"),
                        rs.getString("endpoint"),
                        rs.getLong("request_count"),
                        rs.getLong("error_count"),
                        rs.getDouble("avg_latency_ms"),
                        rs.getDouble("p95_latency_ms"),
                        rs.getDouble("p99_latency_ms")
                ),
                Timestamp.from(windowStart), Timestamp.from(windowEnd)
        );
    }

    public void upsert(Instant windowStart, Instant windowEnd, AggregatedMetric metric) {
        jdbcTemplate.update(
                UPSERT_SQL,
                metric.serviceName(), metric.endpoint(),
                Timestamp.from(windowStart), Timestamp.from(windowEnd),
                metric.requestCount(), metric.errorCount(),
                metric.avgLatencyMs(), metric.p95LatencyMs(), metric.p99LatencyMs()
        );
    }
}
