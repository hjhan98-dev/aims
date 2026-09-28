package com.aims.core.incident;

import com.aims.core.analysis.IncidentAnalysisService;
import com.aims.core.detection.rule.RuleViolation;
import com.aims.core.metric.AggregatedMetric;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentRepository repository;
    private final IncidentAnalysisService incidentAnalysisService;
    private final Clock clock;

    public IncidentService(IncidentRepository repository, IncidentAnalysisService incidentAnalysisService, Clock clock) {
        this.repository = repository;
        this.incidentAnalysisService = incidentAnalysisService;
        this.clock = clock;
    }

    public void recordViolations(AggregatedMetric metric, List<RuleViolation> violations) {
        if (violations.isEmpty()) {
            return;
        }

        Optional<Long> existingIncidentId = repository.findOpenIncidentId(metric.serviceName(), metric.endpoint());
        boolean isNewIncident = existingIncidentId.isEmpty();
        long incidentId = existingIncidentId.orElseGet(() -> repository.createIncident(
                metric.serviceName(), metric.endpoint(), severityOf(violations), clock.instant(),
                summaryOf(violations), metric.windowStart(), metric.windowEnd()));

        for (RuleViolation violation : violations) {
            repository.insertSignal(incidentId, violation);
        }

        // 새로 생성된 Incident에 대해서만 최초 1회 분석 - 기존 열린 Incident에
        // signal만 추가되는 경우에는 재분석하지 않음 (재분석 정책은 이번 범위 밖)
        if (isNewIncident) {
            incidentAnalysisService.analyze(incidentId, metric, violations);
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
