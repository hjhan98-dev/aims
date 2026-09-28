package com.aims.core.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.aims.core.detection.rule.RuleViolation;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncidentPatternClassifierTest {

    private final IncidentPatternClassifier classifier = new IncidentPatternClassifier();

    private static RuleViolation violation(String signalType) {
        return new RuleViolation(signalType, 0.0, 0.0, signalType + " violated");
    }

    @Test
    void latencyOnlyIsClassifiedAsLatencyDegradation() {
        IncidentPattern pattern = classifier.classify(List.of(violation("P95_LATENCY")));
        assertThat(pattern).isEqualTo(IncidentPattern.LATENCY_DEGRADATION);
    }

    @Test
    void errorRateOnlyIsClassifiedAsErrorBurst() {
        IncidentPattern pattern = classifier.classify(List.of(violation("ERROR_RATE")));
        assertThat(pattern).isEqualTo(IncidentPattern.ERROR_BURST);
    }

    @Test
    void exceptionCountOnlyIsClassifiedAsErrorBurst() {
        IncidentPattern pattern = classifier.classify(List.of(violation("EXCEPTION_COUNT")));
        assertThat(pattern).isEqualTo(IncidentPattern.ERROR_BURST);
    }

    @Test
    void latencyAndErrorTogetherIsClassifiedAsCompoundDegradation() {
        IncidentPattern pattern = classifier.classify(List.of(violation("P95_LATENCY"), violation("ERROR_RATE")));
        assertThat(pattern).isEqualTo(IncidentPattern.COMPOUND_DEGRADATION);
    }

    @Test
    void noViolationsFallsBackToUnknown() {
        IncidentPattern pattern = classifier.classify(List.of());
        assertThat(pattern).isEqualTo(IncidentPattern.UNKNOWN);
    }
}
