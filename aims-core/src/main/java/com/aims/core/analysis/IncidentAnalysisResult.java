package com.aims.core.analysis;

import java.util.List;

public record IncidentAnalysisResult(
        IncidentPattern pattern,
        String summary,
        List<String> evidence,
        List<String> recommendedChecks
) {
}
