package com.aims.demo.logging;

import com.aims.demo.scenario.Scenario;
import com.aims.demo.scenario.ScenarioState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String ERROR_TYPE_ATTRIBUTE = "aims.errorType";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "aims.errorMessage";

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("com.aims.demo.access");

    private final ScenarioState scenarioState;
    private final ObjectMapper objectMapper;

    public RequestLoggingFilter(ScenarioState scenarioState, ObjectMapper objectMapper) {
        this.scenarioState = scenarioState;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        Scenario scenarioAtStart = scenarioState.get();
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - start;
            logAccess(request, response, traceId, scenarioAtStart, latencyMs);
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, String traceId,
                            Scenario scenario, long latencyMs) {
        int status = response.getStatus();
        boolean isError = status >= 500;

        StructuredLogEvent event = new StructuredLogEvent(
                Instant.now().toString(),
                "aims-demo",
                isError ? "ERROR" : "INFO",
                traceId,
                request.getMethod(),
                resolvePathTemplate(request),
                status,
                latencyMs,
                scenario.name(),
                isError ? (String) request.getAttribute(ERROR_TYPE_ATTRIBUTE) : null,
                isError ? (String) request.getAttribute(ERROR_MESSAGE_ATTRIBUTE) : null
        );

        try {
            ACCESS_LOG.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            ACCESS_LOG.error("Failed to serialize structured log event", e);
        }
    }

    private String resolvePathTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : request.getRequestURI();
    }
}
