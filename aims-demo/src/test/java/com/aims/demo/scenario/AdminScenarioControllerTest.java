package com.aims.demo.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminScenarioControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ScenarioState scenarioState;

    @AfterEach
    void resetScenario() {
        scenarioState.set(Scenario.NORMAL);
    }

    @Test
    void switchesToSlowDb() {
        restTemplate.postForEntity(url("/admin/scenario/slow-db"), null, String.class);
        assertThat(scenarioState.get()).isEqualTo(Scenario.SLOW_DB);
    }

    @Test
    void switchesToException() {
        restTemplate.postForEntity(url("/admin/scenario/exception"), null, String.class);
        assertThat(scenarioState.get()).isEqualTo(Scenario.EXCEPTION);
    }

    @Test
    void switchesBackToNormal() {
        scenarioState.set(Scenario.EXCEPTION);
        restTemplate.postForEntity(url("/admin/scenario/normal"), null, String.class);
        assertThat(scenarioState.get()).isEqualTo(Scenario.NORMAL);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
