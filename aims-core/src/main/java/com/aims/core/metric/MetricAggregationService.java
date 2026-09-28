package com.aims.core.metric;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MetricAggregationService {

    private static final Logger log = LoggerFactory.getLogger(MetricAggregationService.class);

    private final MetricSnapshotRepository repository;
    private final Clock clock;
    private final long windowMinutes;

    public MetricAggregationService(MetricSnapshotRepository repository,
                                     Clock clock,
                                     @Value("${aims.metric.window-minutes}") long windowMinutes) {
        this.repository = repository;
        this.clock = clock;
        this.windowMinutes = windowMinutes;
    }

    public List<AggregatedMetric> aggregateLastCompletedWindow() {
        WindowRange window = currentWindowRange();
        List<AggregatedMetric> metrics = repository.aggregate(window.start(), window.end());
        for (AggregatedMetric metric : metrics) {
            repository.upsert(window.start(), window.end(), metric);
        }
        log.info("aggregated window [{}, {}): {} endpoint(s)", window.start(), window.end(), metrics.size());
        return metrics;
    }

    /**
     * 가장 최근에 닫힌(방금 마감된) tumbling window. epoch 기준으로 정렬되어 있어서
     * 스케줄러가 언제 시작됐는지와 무관하게 윈도우 경계가 항상 고정됨
     * (예: 1분 윈도우면 항상 매분 :00 경계에 맞춰짐).
     */
    WindowRange currentWindowRange() {
        long windowMillis = Duration.ofMinutes(windowMinutes).toMillis();
        long nowMillis = clock.millis();
        long end = (nowMillis / windowMillis) * windowMillis;
        long start = end - windowMillis;
        return new WindowRange(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end));
    }
}
