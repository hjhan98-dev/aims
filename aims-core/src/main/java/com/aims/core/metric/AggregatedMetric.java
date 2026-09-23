package com.aims.core.metric;

/**
 * 10:03 ~ 10:04
 * service: aims-demo
 * endpoint: /api/flights/{flightNumber}
 * request_count= 5
 * error_count = 1
 * avg_latency  = 784ms
 * p95_latency   = ...
 * p99_latency   = ...
 * @param serviceName
 * @param endpoint
 * @param requestCount
 * @param errorCount
 * @param avgLatencyMs 평균 = 전체적인 상태 (평균적으로 얼마나 느렷나?)
 * @param p95LatencyMs p95: 응답시간을 빠른 순서대로 정렬했을 때 95%의 요청이 이 값 이하로 처리되는 지점 (95%의 요청은 몇 ms 안에 끝났나?)
 * @param p99LatencyMs p99: 4,000ms -> 요청의 약 99%가 4초 이내에 처리됐다 (99$의 요청은?)
 */
public record AggregatedMetric(
        String serviceName,
        String endpoint,
        long requestCount,
        long errorCount,
        double avgLatencyMs,
        double p95LatencyMs,
        double p99LatencyMs
) {
}
