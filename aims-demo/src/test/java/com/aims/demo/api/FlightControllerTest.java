package com.aims.demo.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.aims.demo.api.dto.FlightResponse;
import com.aims.demo.scenario.Scenario;
import com.aims.demo.scenario.ScenarioState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlightControllerTest {

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
    void getFlightReturnsFlightInfo() {
        ResponseEntity<FlightResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/flights/OZ1234", FlightResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().flightNumber()).isEqualTo("OZ1234");
    }
}
