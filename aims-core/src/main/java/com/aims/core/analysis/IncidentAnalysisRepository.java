package com.aims.core.analysis;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentAnalysisRepository {

    private final JdbcTemplate jdbcTemplate;

    public IncidentAnalysisRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(long incidentId, String analysisType, String analysisVersion, String summary,
                        List<String> evidence, List<String> recommendedChecks) {
        jdbcTemplate.update(
                "INSERT INTO incident_analysis (incident_id, analysis_type, analysis_version, summary, evidence, recommended_checks) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                incidentId, analysisType, analysisVersion, summary,
                String.join("\n", evidence), String.join("\n", recommendedChecks));
    }
}
