package com.aims.core.analysis;

import com.aims.core.detection.rule.RuleViolation;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IncidentPatternClassifier {

    public IncidentPattern classify(List<RuleViolation> violations) {
        boolean latencyViolated = violations.stream()
                .anyMatch(v -> v.signalType().equals("P95_LATENCY"));
        boolean errorViolated = violations.stream()
                .anyMatch(v -> v.signalType().equals("ERROR_RATE") || v.signalType().equals("EXCEPTION_COUNT"));

        if (latencyViolated && errorViolated) {
            return IncidentPattern.COMPOUND_DEGRADATION;
        }
        if (latencyViolated) {
            return IncidentPattern.LATENCY_DEGRADATION;
        }
        if (errorViolated) {
            return IncidentPattern.ERROR_BURST;
        }
        return IncidentPattern.UNKNOWN;
    }
}
