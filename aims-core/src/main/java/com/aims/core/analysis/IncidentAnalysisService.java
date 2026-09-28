package com.aims.core.analysis;

import com.aims.core.detection.rule.RuleViolation;
import com.aims.core.metric.AggregatedMetric;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IncidentAnalysisService {

    private static final String ANALYSIS_TYPE = "RULE";
    private static final String ANALYSIS_VERSION = "rule-v1";

    private final IncidentPatternClassifier classifier;
    private final EvidenceAggregator evidenceAggregator;
    private final IncidentAnalysisRepository repository;

    public IncidentAnalysisService(IncidentPatternClassifier classifier,
                                    EvidenceAggregator evidenceAggregator,
                                    IncidentAnalysisRepository repository) {
        this.classifier = classifier;
        this.evidenceAggregator = evidenceAggregator;
        this.repository = repository;
    }

    public void analyze(long incidentId, AggregatedMetric metric, List<RuleViolation> violations) {
        IncidentPattern pattern = classifier.classify(violations);
        List<String> evidence = evidenceAggregator.aggregate(metric, violations);
        List<String> recommendedChecks = recommendedChecksFor(pattern);
        String summary = summaryFor(pattern);

        repository.insert(incidentId, ANALYSIS_TYPE, ANALYSIS_VERSION, summary, evidence, recommendedChecks);
    }

    private String summaryFor(IncidentPattern pattern) {
        return switch (pattern) {
            case LATENCY_DEGRADATION -> "응답 지연 임계치가 초과되었습니다.";
            case ERROR_BURST -> "오류율/예외 건수 임계치가 초과되었습니다.";
            case COMPOUND_DEGRADATION -> "응답 지연과 오류율 임계치가 동시에 초과되었습니다.";
            case UNKNOWN -> "알려진 패턴과 일치하지 않는 위반입니다.";
        };
    }

    private List<String> recommendedChecksFor(IncidentPattern pattern) {
        return switch (pattern) {
            case LATENCY_DEGRADATION -> List.of(
                    "다운스트림/DB 응답 시간 확인",
                    "리소스(CPU/커넥션 풀) 점검");
            case ERROR_BURST -> List.of(
                    "예외 메시지 원문 확인",
                    "최근 배포/설정 변경 여부 확인");
            case COMPOUND_DEGRADATION -> List.of(
                    "다운스트림 연계 상태 확인",
                    "DB/외부 API 응답 시간 확인",
                    "최근 배포 및 설정 변경 확인");
            case UNKNOWN -> List.of("수동 확인 필요");
        };
    }
}
