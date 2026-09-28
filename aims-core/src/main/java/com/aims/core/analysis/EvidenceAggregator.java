package com.aims.core.analysis;

import com.aims.core.detection.rule.RuleViolation;
import com.aims.core.metric.AggregatedMetric;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EvidenceAggregator {

    private static final int MAX_EXCEPTION_SAMPLES = 3;

    private final JdbcTemplate jdbcTemplate;

    public EvidenceAggregator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> aggregate(AggregatedMetric metric, List<RuleViolation> violations) {
        List<String> evidence = new ArrayList<>();

        evidence.add("Request Count: %d".formatted(metric.requestCount()));
        evidence.add("Error Count: %d".formatted(metric.errorCount()));
        if (metric.requestCount() > 0) {
            double errorRate = (double) metric.errorCount() / metric.requestCount() * 100.0;
            evidence.add("Error Rate: %.2f%%".formatted(errorRate));
        }
        evidence.add("Avg Latency: %.0fms".formatted(metric.avgLatencyMs()));
        evidence.add("P95 Latency: %.0fms".formatted(metric.p95LatencyMs()));
        evidence.add("P99 Latency: %.0fms".formatted(metric.p99LatencyMs()));

        for (RuleViolation violation : violations) {
            evidence.add("Triggered Signal: %s (%s)".formatted(violation.signalType(), violation.description()));
        }

        for (String sample : exceptionSamples(metric)) {
            evidence.add(sample);
        }

        return evidence;
    }

    // 대표 예외 샘플은 반드시 Incident가 속한 윈도우(window_start~window_end) +
    // 해당 endpoint 범위 안에서만 조회함 - 원본 로그를 무분별하게 끌어오지 않고
    // 이 Incident와 직접 관련된 것만 근거로 사용
    private List<String> exceptionSamples(AggregatedMetric metric) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DISTINCT exception_type, exception_message FROM service_log "
                        + "WHERE service_name = ? AND endpoint = ? AND timestamp >= ? AND timestamp < ? "
                        + "AND exception_type IS NOT NULL LIMIT ?",
                metric.serviceName(), metric.endpoint(),
                Timestamp.from(metric.windowStart()), Timestamp.from(metric.windowEnd()),
                MAX_EXCEPTION_SAMPLES);

        List<String> samples = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            samples.add("Exception Sample: %s - %s".formatted(row.get("exception_type"), row.get("exception_message")));
        }
        return samples;
    }
}
