package com.aims.core.detection.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.aims.core.metric.AggregatedMetric;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private static final Instant WINDOW_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-01-01T00:01:00Z");

    private final RuleEngine engine = new RuleEngine(List.of(
            new ErrorRateRule(5.0),
            new P95LatencyRule(2000),
            new ExceptionCountRule(10)
    ));

    private static AggregatedMetric metric(String endpoint, long requestCount, long errorCount,
                                            double avgLatencyMs, double p95LatencyMs, double p99LatencyMs) {
        return new AggregatedMetric("aims-demo", endpoint, requestCount, errorCount,
                avgLatencyMs, p95LatencyMs, p99LatencyMs, WINDOW_START, WINDOW_END);
    }

    @Test
    void noViolationWhenAllMetricsAreHealthy() {
        AggregatedMetric metric = metric("/api/flights/{x}", 100, 1, 50.0, 100.0, 150.0);
        assertThat(engine.evaluate(metric)).isEmpty();
    }

    @Test
    void errorRateAboveThresholdIsDetected() {
        AggregatedMetric metric = metric("/api/baggage", 100, 10, 50.0, 100.0, 150.0);
        List<RuleViolation> violations = engine.evaluate(metric);
        assertThat(violations).extracting(RuleViolation::signalType).contains("ERROR_RATE");
    }

    @Test
    void p95LatencyAboveThresholdIsDetected() {
        AggregatedMetric metric = metric("/api/flights/{x}", 10, 0, 500.0, 3100.0, 3200.0);
        List<RuleViolation> violations = engine.evaluate(metric);
        assertThat(violations).extracting(RuleViolation::signalType).containsExactly("P95_LATENCY");
    }

    @Test
    void exceptionCountAboveThresholdIsDetectedEvenWithLowErrorRate() {
        // 500 requests, 11 errors -> error rate 2.2% (under 5% threshold) but raw count(11) is over 10
        AggregatedMetric metric = metric("/api/baggage", 500, 11, 50.0, 100.0, 150.0);
        List<RuleViolation> violations = engine.evaluate(metric);
        assertThat(violations).extracting(RuleViolation::signalType).containsExactly("EXCEPTION_COUNT");
    }

    @Test
    void zeroRequestsNeverTriggersErrorRateRule() {
        AggregatedMetric metric = metric("/api/flights/{x}", 0, 0, 0.0, 0.0, 0.0);
        assertThat(engine.evaluate(metric)).isEmpty();
    }
}
