package com.aims.core.metric;

import com.aims.core.detection.rule.RuleEngine;
import com.aims.core.detection.rule.RuleViolation;
import com.aims.core.incident.IncidentService;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricAggregationScheduler {

    private final MetricAggregationService metricAggregationService;
    private final RuleEngine ruleEngine;
    private final IncidentService incidentService;

    public MetricAggregationScheduler(MetricAggregationService metricAggregationService,
                                       RuleEngine ruleEngine,
                                       IncidentService incidentService) {
        this.metricAggregationService = metricAggregationService;
        this.ruleEngine = ruleEngine;
        this.incidentService = incidentService;
    }

    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void run() {
        List<AggregatedMetric> metrics = metricAggregationService.aggregateLastCompletedWindow();
        for (AggregatedMetric metric : metrics) {
            List<RuleViolation> violations = ruleEngine.evaluate(metric);
            incidentService.recordViolations(metric, violations);
        }
    }
}
