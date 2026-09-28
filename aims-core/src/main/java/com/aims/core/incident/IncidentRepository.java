package com.aims.core.incident;

import com.aims.core.detection.rule.RuleViolation;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentRepository {

    private final JdbcTemplate jdbcTemplate;

    public IncidentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findOpenIncidentId(String serviceName, String endpoint) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM incident WHERE service_name = ? AND endpoint = ? AND status != 'RESOLVED' "
                        + "ORDER BY detected_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), serviceName, endpoint);
        return ids.stream().findFirst();
    }

    public long createIncident(String serviceName, String endpoint, String severity, Instant detectedAt,
                                String summary, Instant windowStart, Instant windowEnd) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO incident (service_name, endpoint, severity, status, detected_at, summary, "
                            + "window_start, window_end) VALUES (?, ?, ?, 'DETECTED', ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, serviceName);
            ps.setString(2, endpoint);
            ps.setString(3, severity);
            ps.setTimestamp(4, Timestamp.from(detectedAt));
            ps.setString(5, summary);
            ps.setTimestamp(6, Timestamp.from(windowStart));
            ps.setTimestamp(7, Timestamp.from(windowEnd));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void insertSignal(long incidentId, RuleViolation violation) {
        jdbcTemplate.update(
                "INSERT INTO incident_signal (incident_id, signal_type, signal_value, threshold, description) "
                        + "VALUES (?, ?, ?, ?, ?)",
                incidentId, violation.signalType(), violation.signalValue(), violation.threshold(), violation.description());
    }
}
