package com.aims.demo.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aims.demo.scenario.Scenario;
import com.aims.demo.scenario.ScenarioState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestLoggingFilterTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ScenarioState scenarioState;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger accessLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        scenarioState.set(Scenario.NORMAL);
        accessLogger = (Logger) LoggerFactory.getLogger("com.aims.demo.access");
        listAppender = new ListAppender<>();
        listAppender.start();
        accessLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(listAppender);
        scenarioState.set(Scenario.NORMAL);
    }

    @Test
    void logsRouteTemplateInsteadOfRawPath() throws Exception {
        restTemplate.getForEntity(url("/api/flights/OZ1234"), String.class);

        JsonNode event = onlyLoggedEvent();
        assertThat(event.get("path").asText()).isEqualTo("/api/flights/{flightNumber}");
        assertThat(event.get("method").asText()).isEqualTo("GET");
        assertThat(event.get("status").asInt()).isEqualTo(200);
        assertThat(event.get("level").asText()).isEqualTo("INFO");
        assertThat(event.get("scenario").asText()).isEqualTo("NORMAL");
        assertThat(event.get("latencyMs").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(event.get("service").asText()).isEqualTo("aims-demo");
        assertThat(event.get("traceId").asText()).isNotBlank();
    }

    @Test
    void logsErrorLevelWithErrorDetailsWhenExceptionScenarioActive() throws Exception {
        scenarioState.set(Scenario.EXCEPTION);

        restTemplate.getForEntity(url("/api/flights/OZ1234"), String.class);

        JsonNode event = onlyLoggedEvent();
        assertThat(event.get("status").asInt()).isEqualTo(500);
        assertThat(event.get("level").asText()).isEqualTo("ERROR");
        assertThat(event.get("errorType").asText()).isEqualTo("SimulatedFailureException");
        assertThat(event.get("errorMessage").asText()).isNotBlank();
    }

    @Test
    void doesNotLogAdminScenarioCalls() {
        restTemplate.postForEntity(url("/admin/scenario/normal"), null, String.class);

        assertThat(listAppender.list).isEmpty();
    }

    private JsonNode onlyLoggedEvent() throws Exception {
        assertThat(listAppender.list).hasSize(1);
        String json = listAppender.list.get(0).getFormattedMessage();
        return objectMapper.readTree(json);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
