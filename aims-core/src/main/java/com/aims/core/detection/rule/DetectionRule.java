package com.aims.core.detection.rule;

import com.aims.core.metric.AggregatedMetric;
import java.util.Optional;

public interface DetectionRule {
    Optional<RuleViolation> evaluate(AggregatedMetric metric);
}
