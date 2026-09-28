package com.aims.core.detection.rule;

import com.aims.core.metric.AggregatedMetric;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ErrorRateRule implements DetectionRule {

    private final double thresholdPercent;

    public ErrorRateRule(@Value("${aims.rule.error-rate-threshold}") double thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    @Override
    public Optional<RuleViolation> evaluate(AggregatedMetric metric) {
        if (metric.requestCount() == 0) {
            return Optional.empty();
        }
        double errorRate = (double) metric.errorCount() / metric.requestCount() * 100.0;
        if (errorRate > thresholdPercent) {
            return Optional.of(new RuleViolation(
                    "ERROR_RATE", errorRate, thresholdPercent,
                    "Error rate %.2f%% exceeded threshold %.2f%%".formatted(errorRate, thresholdPercent)));
        }
        return Optional.empty();
    }
}
