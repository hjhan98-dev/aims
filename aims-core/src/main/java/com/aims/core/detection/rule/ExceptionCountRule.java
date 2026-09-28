package com.aims.core.detection.rule;

import com.aims.core.metric.AggregatedMetric;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExceptionCountRule implements DetectionRule {

    private final double threshold;

    public ExceptionCountRule(@Value("${aims.rule.exception-count-threshold}") double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Optional<RuleViolation> evaluate(AggregatedMetric metric) {
        if (metric.errorCount() > threshold) {
            return Optional.of(new RuleViolation(
                    "EXCEPTION_COUNT", metric.errorCount(), threshold,
                    "Exception count %d exceeded threshold %.0f".formatted(metric.errorCount(), threshold)));
        }
        return Optional.empty();
    }
}
