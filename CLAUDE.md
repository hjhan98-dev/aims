# AIMS — AI Monitoring & Incident Analysis System

## 1. Project Overview

AIMS는 운영 서비스에서 발생하는 로그와 성능 지표를 실시간으로 수집하고, 이상 징후를 탐지한 뒤 AI를 활용하여 장애 상황과 가능한 원인을 분석하고 대응 정보를 제공하는 AI 기반 운영 모니터링 플랫폼이다.

핵심 목표는 단순한 로그 조회 시스템이 아니라 다음의 전체 흐름을 실제 서비스 수준으로 구현하는 것이다.

```text
Demo Service
    ↓
Structured Log
    ↓
Log Collector
    ↓
PostgreSQL
    ↓
Detection Engine
    ↓
Rule / Statistical Anomaly Detection
    ↓
AI Analysis
    ↓
Incident
    ↓
Slack / SMS Alert
```

---

## 2. Project Purpose

이 프로젝트는 포트폴리오 및 기술 면접을 목적으로 한다.

특히 다음 역량을 보여주는 것을 목표로 한다.

* Backend API development
* Data collection pipeline
* PostgreSQL schema design
* Real-time log processing
* Anomaly detection
* AI/LLM API serving
* Incident management
* Docker containerization
* AWS deployment
* Performance testing
* Failure scenario testing
* Production-oriented backend design

AI 모델 자체를 연구하는 프로젝트가 아니다.

AI를 실제 서비스에 연결하기 위한 Backend/Data/Operations 계층을 구현하는 것이 핵심이다.

---

## 3. Core Design Principles

### 3.1 AI가 장애 여부를 단독으로 판단하지 않는다.

명확하게 정의할 수 있는 장애 조건은 Rule Engine이 판단한다.

예:

* HTTP 5xx rate threshold exceeded
* Exception count threshold exceeded
* P95 latency threshold exceeded
* Timeout count threshold exceeded
* MQ backlog threshold exceeded

AI는 탐지된 Incident의 여러 Signal을 종합하여 다음 정보를 제공한다.

* 장애 요약
* 가능한 원인
* 판단 근거
* 추가 확인이 필요한 항목
* 대응 권장사항

즉:

```text
Rule / Statistical Detection
        ↓
Incident Signal
        ↓
AI Analysis
```

구조를 유지한다.

---

### 3.2 AI API에 원본 로그 전체를 무분별하게 전달하지 않는다.

AI에 전달하기 전에 Backend에서 로그와 Metric을 집계하고 구조화한다.

Bad:

```text
10000 raw logs
    ↓
LLM
```

Good:

```text
Raw Logs
    ↓
Aggregation
    ↓
Incident Signals
    ↓
Structured Context
    ↓
LLM
```

토큰 비용, 개인정보/민감정보 노출, 분석 안정성을 고려한다.

---

### 3.3 실제 운영 로그를 외부 AI API에 직접 전송하지 않는다.

실제 회사 운영 로그에는 개인정보, 내부 IP, 시스템 정보, 승객 관련 데이터 등이 포함될 수 있으므로 프로젝트에서는 일반화된 Demo Log와 Synthetic Data를 사용한다.

실제 업무 경험에서 발생했던 장애 패턴만 일반화하여 재현한다.

---

### 3.4 기술을 추가하기 위한 기술을 사용하지 않는다.

프로젝트의 핵심 흐름을 단순하게 유지한다.

초기 구현에서는 다음 기술을 필수로 요구하지 않는다.

* Kafka
* Kubernetes
* Redis
* Elasticsearch
* Milvus
* Terraform
* 복잡한 MSA
* 복잡한 ML model

필요성이 명확해질 때만 추가한다.

---

## 4. Technology Stack

### Backend

* Java
* Spring Boot
* Spring Data JPA
* QueryDSL
* Gradle
* Lombok

Java 버전은 프로젝트 시작 시 최신 LTS 버전을 기준으로 선택하되, 선택한 버전과 이유를 문서화한다.

### Data Collection

* Python
* FastAPI 또는 적절한 lightweight collector framework
* PostgreSQL

Collector는 로그 수집과 정규화에 집중한다.

### AI

* External LLM API
* Structured JSON output
* Prompt version management

LLM provider에 강하게 종속되는 구조를 피한다.

LLM 호출 부분은 별도 interface/adapter 계층으로 분리한다.

예:

```text
AiAnalyzer
    ↓
LlmClient
    ↓
OpenAI / Other LLM Provider
```

### Infrastructure

* Docker
* Docker Compose
* AWS EC2
* AWS RDS PostgreSQL

초기에는 EC2 + Docker Compose를 우선 구현한다.

ECS 등 추가 AWS 서비스는 핵심 기능 완성 후 필요성을 검토한다.

### Testing

* JUnit 5
* Mockito
* Spring Boot Test
* JMeter 또는 적절한 load testing tool

---

## 5. Demo Service

실제 회사 시스템과 분리된 Demo Service를 구현한다.

항공/공항 업무 경험을 일반화한 시스템으로 구성한다.

예시 API:

```text
POST /api/passengers/checkin
GET  /api/flights/{flightId}
POST /api/baggage
GET  /api/seat-map/{flightId}
```

Demo Service는 구조화된 JSON 로그를 생성한다.

또한 장애 상황을 재현할 수 있는 Admin API를 제공한다.

예:

```text
POST /admin/scenario/normal
POST /admin/scenario/slow-db
POST /admin/scenario/exception
POST /admin/scenario/external-timeout
POST /admin/scenario/mq-delay
```

Admin API는 개발/테스트 환경에서만 활성화한다.

---

## 6. Log Format

가능하면 JSON structured logging을 사용한다.

예:

```json
{
  "timestamp": "2026-09-18T10:31:21.123Z",
  "service": "aims-demo",
  "level": "ERROR",
  "traceId": "abc123",
  "method": "POST",
  "endpoint": "/api/passengers/checkin",
  "statusCode": 500,
  "responseTimeMs": 3280,
  "exceptionType": "TimeoutException",
  "exceptionMessage": "External API timeout"
}
```

로그 구조는 Detection Engine에서 쉽게 집계할 수 있도록 설계한다.

---

## 7. Database Design

초기 핵심 테이블은 다음과 같다.

### service_log

```text
id
timestamp
service_name
trace_id
log_level
endpoint
status_code
response_time_ms
exception_type
exception_message
created_at
```

### metric_snapshot

```text
id
service_name
endpoint
window_start
window_end
request_count
error_count
avg_latency_ms
p95_latency_ms
p99_latency_ms
```

### incident

```text
id
service_name
endpoint
severity
status
detected_at
resolved_at
summary
```

### incident_signal

```text
id
incident_id
signal_type
signal_value
threshold
description
```

### ai_analysis

```text
id
incident_id
model
prompt_version
analysis
suspected_cause
recommendation
created_at
```

필요한 경우 설계를 수정할 수 있지만, 변경 이유를 문서화한다.

---

## 8. Detection Strategy

Detection은 세 단계로 구성한다.

### Level 1 — Rule Detection

예:

```text
5xx rate > 5%
Exception count > 10/min
P95 latency > 2000ms
Timeout count > 5/min
```

Threshold는 설정값으로 관리한다.

하드코딩하지 않는다.

---

### Level 2 — Statistical Anomaly Detection

초기 구현에서는 복잡한 ML 모델을 사용하지 않는다.

Rolling average, standard deviation, Z-score 등 설명 가능한 통계 방법을 우선 사용한다.

예:

```text
Baseline latency
    ↓
Current latency
    ↓
Deviation calculation
    ↓
Anomaly score
```

탐지 알고리즘과 계산 로직은 별도의 domain/service 계층으로 분리한다.

---

### Level 3 — AI Analysis

Rule 또는 Statistical Detection을 통해 Incident 후보가 생성되면 AI Analyzer가 분석한다.

AI Input:

```json
{
  "service": "aims-demo",
  "endpoint": "/api/passengers/checkin",
  "windowMinutes": 10,
  "signals": {
    "requestCount": 1850,
    "errorRate": 8.2,
    "p95Latency": 3100,
    "timeoutCount": 27,
    "exceptionCount": 43
  }
}
```

AI Output은 반드시 구조화된 JSON으로 관리한다.

예:

```json
{
  "severity": "CRITICAL",
  "summary": "...",
  "suspectedCauses": [],
  "evidence": [],
  "recommendedChecks": []
}
```

AI가 생성한 내용을 시스템의 사실 데이터와 구분한다.

---

## 9. Incident Lifecycle

Incident 상태는 명확하게 관리한다.

```text
DETECTED
    ↓
ANALYZING
    ↓
NOTIFIED
    ↓
ACKNOWLEDGED
    ↓
RESOLVED
```

중복 Incident 생성 방지 전략을 구현한다.

같은 서비스/endpoint에 동일한 장애가 지속되는 경우 무분별하게 Incident를 생성하지 않는다.

---

## 10. Alert

초기에는 Slack을 우선 구현한다.

Critical Incident:

```text
🚨 CRITICAL INCIDENT

Service: aims-demo
Endpoint: POST /api/passengers/checkin

P95 Latency: 3100ms
Error Rate: 8.2%
Timeout: 27

AI Analysis:
External API response delay is suspected.

Recommended Checks:
1. Check external API response time
2. Check DB connection pool
```

SMS는 핵심 기능 완성 후 추가한다.

---

## 11. Performance Requirements

프로젝트는 기능 구현만으로 끝내지 않는다.

다음 지표를 측정한다.

* Log ingestion throughput
* Detection latency
* API response time
* P95 / P99
* DB query performance
* AI analysis latency
* Alert delivery latency

측정 결과를 README에 기록한다.

---

## 12. Failure Scenarios

최소 다음 장애를 재현하고 탐지한다.

1. Exception Spike
2. API Latency Increase
3. External API Timeout
4. Database Bottleneck
5. Message Processing Delay

각 시나리오마다 다음을 문서화한다.

```text
Scenario
→ Expected Signal
→ Detection Result
→ AI Analysis
→ Alert
```

---

## 13. AI Evaluation

AI 기능은 단순히 "호출 성공"으로 평가하지 않는다.

Synthetic Incident Dataset을 생성한다.

최소 20~30개의 장애 시나리오를 만들고 다음 항목을 평가한다.

* Incident type classification
* Suspected cause identification
* Evidence inclusion
* Recommended action relevance
* Hallucination / unsupported claim

측정 결과를 문서화한다.

---

## 14. Coding Rules

### General

* 과도한 abstraction을 만들지 않는다.
* 실제 요구사항이 없는 generic framework를 만들지 않는다.
* 명확한 이름을 사용한다.
* 하나의 클래스에 너무 많은 책임을 넣지 않는다.
* 변경 이유가 있는 경우 주석보다 문서/코드 구조로 표현한다.

### Java

* Spring Boot convention을 따른다.
* Domain logic은 Controller에 작성하지 않는다.
* Controller → Application/Service → Domain/Repository 구조를 기본으로 한다.
* DTO와 Entity를 구분한다.
* Transaction boundary를 명확하게 한다.
* N+1 문제를 고려한다.
* 불필요한 Optional 남용을 피한다.
* null 처리에서는 프로젝트 전체에서 일관된 방식을 사용한다.
* Builder pattern을 필요한 경우 활용한다.

### Database

* 적절한 index를 설계한다.
* Query 성능을 확인한다.
* 대량 데이터 처리 시 pagination/batch를 고려한다.
* schema 변경은 migration으로 관리한다.

### Python

* Collector는 단순하게 유지한다.
* 수집 / 정규화 / 저장 책임을 구분한다.
* 실패한 로그 처리 전략을 정의한다.

---

## 15. Development Process

구현은 다음 순서로 진행한다.

### Phase 1

Architecture + Repository structure + Database schema

### Phase 2

Demo Service

### Phase 3

Log Collector

### Phase 4

Metric Aggregation

### Phase 5

Rule Detection

### Phase 6

Statistical Anomaly Detection

### Phase 7

AI Analysis

### Phase 8

Incident + Alert

### Phase 9

Docker

### Phase 10

AWS deployment

### Phase 11

Load Test + AI Evaluation

### Phase 12

Documentation + Portfolio

각 Phase를 완료하기 전에 테스트와 실행 방법을 확인한다.

---

## 16. Important Claude Code Rules

구현 전에 현재 프로젝트 구조와 요구사항을 먼저 분석한다.

한 번에 대규모 코드를 생성하지 않는다.

각 Phase별로:

1. 구현 계획 제시
2. 변경 파일 목록 제시
3. 구현
4. 테스트
5. 실행 확인
6. 변경사항 요약
7. 다음 Phase 제안

순서로 진행한다.

기존 코드와 충돌하는 경우 임의로 덮어쓰지 않는다.

중요한 아키텍처 결정을 변경할 경우 먼저 이유를 설명한다.

요구사항에 없는 기술을 추가하려면 먼저 필요성과 trade-off를 설명한다.

"작동하는 코드"뿐 아니라 "왜 이렇게 설계했는지 설명 가능한 코드"를 작성한다.

모든 핵심 기능에는 테스트를 작성한다.

프로젝트의 목적은 기술 스택을 많이 보여주는 것이 아니라 실제 문제를 해결하는 과정을 보여주는 것이다.
