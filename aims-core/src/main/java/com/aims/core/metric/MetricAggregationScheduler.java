package com.aims.core.metric;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricAggregationScheduler {

    private final MetricAggregationService metricAggregationService;

    public MetricAggregationScheduler(MetricAggregationService metricAggregationService) {
        this.metricAggregationService = metricAggregationService;
    }

    @Scheduled(cron = "0 * * * * *") // 매 분 0초에 실행
    public void run() {
        metricAggregationService.aggregateLastCompletedWindow();
    }
}
