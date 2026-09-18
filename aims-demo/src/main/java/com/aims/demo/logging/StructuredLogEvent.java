package com.aims.demo.logging;

public record StructuredLogEvent(
        String timestamp,
        String service,
        String level,
        String traceId,
        String method,
        String path,
        int status,
        long latencyMs,
        String scenario,
        String errorType,
        String errorMessage
) {
}
