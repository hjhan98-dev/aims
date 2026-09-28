package com.aims.core.detection.rule;

import com.aims.core.metric.AggregatedMetric;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {

    private final List<DetectionRule> rules;

    public RuleEngine(List<DetectionRule> rules) {
        this.rules = rules;
    }

    public List<RuleViolation> evaluate(AggregatedMetric metric) {
        return rules.stream()
                .map(rule -> rule.evaluate(metric))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
