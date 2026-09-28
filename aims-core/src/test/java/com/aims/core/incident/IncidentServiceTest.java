package com.aims.core.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.aims.core.detection.rule.RuleViolation;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// AimsCoreApplicationTests와 마찬가지로 로컬에 PostgreSQL이 떠 있어야 함
@SpringBootTest
class IncidentServiceTest {

    private static final String SERVICE_NAME = "test-incident-svc";
    private static final String ENDPOINT = "/api/test";

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM incident_signal WHERE incident_id IN (SELECT id FROM incident WHERE service_name = ?)",
                SERVICE_NAME);
        jdbcTemplate.update("DELETE FROM incident WHERE service_name = ?", SERVICE_NAME);
    }

    @Test
    void noIncidentIsCreatedWhenThereAreNoViolations() {
        incidentService.recordViolations(SERVICE_NAME, ENDPOINT, List.of());

        Integer count = countIncidents();
        assertThat(count).isZero();
    }

    @Test
    void repeatedViolationsForSameEndpointReuseTheSameOpenIncidentInsteadOfCreatingANewOne() {
        RuleViolation first = new RuleViolation("P95_LATENCY", 3100.0, 2000.0, "P95 latency 3100ms exceeded threshold 2000ms");
        RuleViolation second = new RuleViolation("ERROR_RATE", 8.0, 5.0, "Error rate 8.00% exceeded threshold 5.00%");

        incidentService.recordViolations(SERVICE_NAME, ENDPOINT, List.of(first));
        incidentService.recordViolations(SERVICE_NAME, ENDPOINT, List.of(second));

        assertThat(countIncidents()).isEqualTo(1);
        Integer signalCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM incident_signal isg JOIN incident i ON i.id = isg.incident_id "
                        + "WHERE i.service_name = ?",
                Integer.class, SERVICE_NAME);
        assertThat(signalCount).isEqualTo(2);

        String severity = jdbcTemplate.queryForObject(
                "SELECT severity FROM incident WHERE service_name = ?", String.class, SERVICE_NAME);
        assertThat(severity).isEqualTo("WARNING"); // 최초 생성 시점(first만 있을 때) 기준
    }

    private Integer countIncidents() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM incident WHERE service_name = ?", Integer.class, SERVICE_NAME);
    }
}
