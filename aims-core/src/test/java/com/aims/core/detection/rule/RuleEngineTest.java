package com.aims.core.detection.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.aims.core.metric.AggregatedMetric;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine(List.of(
            new ErrorRateRule(5.0),
            new P95LatencyRule(2000),
            new ExceptionCountRule(10)
    ));

    @Test
    void noViolationWhenAllMetricsAreHealthy() {
        AggregatedMetric metric = new AggregatedMetric("aims-demo", "/api/flights/{x}", 100, 1, 50.0, 100.0, 150.0);
        assertThat(engine.evaluate(metric)).isEmpty();
    }

    @Test
    void errorRateAboveThresholdIsDetected() {
        AggregatedMetric metric = new AggregatedMetric("aims-demo", "/api/baggage", 100, 10, 50.0, 100.0, 150.0);
        List<RuleViolation> violations = engine.evaluate(metric);
        assertThat(violations).extracting(RuleViolation::signalType).contains("ERROR_RATE");
    }

    @Test
    void p95LatencyAboveThresholdIsDetected() {
        AggregatedMetric metric = new AggregatedMetric("aims-demo", "/api/flights/{x}", 10, 0, 500.0, 3100.0, 3200.0);
        List<RuleViolation> violations = engine.evaluate(metric);
        assertThat(violations).extracting(RuleViolation::signalType).containsExactly("P95_LATENCY");
    }

    @Test
    void exceptionCountAboveThresholdIsDetectedEvenWithLowErrorRate() {
        // 500 requests, 11 errors -> error rate 2.2% (under 5% threshold) but raw count(11) is over 10
        AggregatedMetric metric = new AggregatedMetric("aims-demo", "/api/baggage", 500, 11, 50.0, 100.0, 150.0);
        List<RuleViolation> violations = engine.evaluate(metric);
        assertThat(violations).extracting(RuleViolation::signalType).containsExactly("EXCEPTION_COUNT");
    }

    @Test
    void zeroRequestsNeverTriggersErrorRateRule() {
        AggregatedMetric metric = new AggregatedMetric("aims-demo", "/api/flights/{x}", 0, 0, 0.0, 0.0, 0.0);
        assertThat(engine.evaluate(metric)).isEmpty();
    }
}
