package com.aims.core.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MetricAggregationServiceTest {

    @Test
    void oneMinuteWindowAlignsToTheMinuteBoundary() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:05:23Z"), ZoneOffset.UTC);
        MetricAggregationService service = new MetricAggregationService(
                mock(MetricSnapshotRepository.class), clock, 1);

        WindowRange window = service.currentWindowRange();

        assertThat(window.start()).isEqualTo(Instant.parse("2026-09-21T10:04:00Z"));
        assertThat(window.end()).isEqualTo(Instant.parse("2026-09-21T10:05:00Z"));
    }

    @Test
    void fiveMinuteWindowAlignsToEpochNotToServiceStartTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:07:59Z"), ZoneOffset.UTC);
        MetricAggregationService service = new MetricAggregationService(
                mock(MetricSnapshotRepository.class), clock, 5);

        WindowRange window = service.currentWindowRange();

        assertThat(window.start()).isEqualTo(Instant.parse("2026-09-21T10:00:00Z"));
        assertThat(window.end()).isEqualTo(Instant.parse("2026-09-21T10:05:00Z"));
    }

    @Test
    void exactBoundaryInstantClosesTheWindowEndingAtThatInstant() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:06:00Z"), ZoneOffset.UTC);
        MetricAggregationService service = new MetricAggregationService(
                mock(MetricSnapshotRepository.class), clock, 1);

        WindowRange window = service.currentWindowRange();

        assertThat(window.start()).isEqualTo(Instant.parse("2026-09-21T10:05:00Z"));
        assertThat(window.end()).isEqualTo(Instant.parse("2026-09-21T10:06:00Z"));
    }
}
