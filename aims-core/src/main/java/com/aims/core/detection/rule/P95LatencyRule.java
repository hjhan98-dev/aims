package com.aims.core.detection.rule;

import com.aims.core.metric.AggregatedMetric;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class P95LatencyRule implements DetectionRule {

    private final double thresholdMs;

    public P95LatencyRule(@Value("${aims.rule.p95-latency-threshold-ms}") double thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public Optional<RuleViolation> evaluate(AggregatedMetric metric) {
        if (metric.p95LatencyMs() > thresholdMs) {
            return Optional.of(new RuleViolation(
                    "P95_LATENCY", metric.p95LatencyMs(), thresholdMs,
                    "P95 latency %.0fms exceeded threshold %.0fms".formatted(metric.p95LatencyMs(), thresholdMs)));
        }
        return Optional.empty();
    }
}
