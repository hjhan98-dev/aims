package com.aims.core.incident;

import com.aims.core.detection.rule.RuleViolation;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentRepository repository;
    private final Clock clock;

    public IncidentService(IncidentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void recordViolations(String serviceName, String endpoint, List<RuleViolation> violations) {
        if (violations.isEmpty()) {
            return;
        }

        long incidentId = repository.findOpenIncidentId(serviceName, endpoint)
                .orElseGet(() -> repository.createIncident(
                        serviceName, endpoint, severityOf(violations), clock.instant(), summaryOf(violations)));

        for (RuleViolation violation : violations) {
            repository.insertSignal(incidentId, violation);
        }
    }

    private String severityOf(List<RuleViolation> violations) {
        boolean hasErrorRateViolation = violations.stream()
                .anyMatch(v -> v.signalType().equals("ERROR_RATE"));
        return hasErrorRateViolation ? "CRITICAL" : "WARNING";
    }

    private String summaryOf(List<RuleViolation> violations) {
        return violations.get(0).description();
    }
}
