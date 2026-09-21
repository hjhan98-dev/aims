# AIMS — AI Monitoring & Incident Analysis System

## 1. 프로젝트 소개

AIMS는 운영 서비스의 로그와 성능 지표를 실시간으로 수집하고, 이상 징후를 탐지한 뒤 AI가 장애 상황과 가능한 원인을 분석하여 대응 정보를 제공하는 AI 기반 운영 모니터링 플랫폼입니다.

이 프로젝트는 "AI가 장애 여부를 단독으로 판단하지 않는다"는 원칙을 따릅니다. 장애 후보는 명확한 Rule 기반 탐지가 먼저 판단하고, AI는 그 결과(Signal)를 넘겨받아 원인 분석과 대응 정보를 제공하는 역할만 맡습니다. 또한 원본 로그 전체를 LLM에 그대로 전달하지 않고, Backend에서 집계·구조화한 정보만 전달하도록 설계합니다.

핵심 데이터 흐름:

```text
감시 대상 운영 서비스
        ↓
Structured JSON Log
        ↓
Log Collector
        ↓
PostgreSQL
        ↓
Metric Aggregation
        ↓
Rule Detection
        ↓
Incident
        ↓
AI Analysis
        ↓
Alert
```

이 흐름 전체를 실제로 동작하는 백엔드 시스템으로 구현하는 것이 이 프로젝트의 목표이며, 현재는 그 중 **Phase 1(프로젝트/DB 기반 구성)** 까지만 진행된 상태입니다.

---

## 2. 현재 Phase 1 상태

Phase 1은 이후 모든 기능이 얹힐 프로젝트 구조와 DB 기반을 준비하는 단계이며, **실제 로그 수집/탐지/AI 분석 기능은 아직 구현되지 않았습니다.**

Phase 1에서 완료한 것:

- monorepo 구조 (`aims-demo` / `aims-core` / `aims-collector` / `infra`)
- `aims-demo` skeleton (Spring Boot, 부팅만 가능, 업무 API 없음)
- `aims-core` skeleton (Spring Boot, PostgreSQL/Flyway 연결 구성, 도메인 로직 없음)
- `aims-collector` skeleton (Python, 실행 진입점만 존재, 수집 로직 없음)
- PostgreSQL Docker Compose 정의
- Flyway `V1__init_schema.sql` 작성 (5개 테이블 정의)
- Java 17 / Spring Boot 3.3.4 기반 확정
- Python collector 최소 구조

---

## 3. 서비스 책임

```text
aims-demo
= AIMS가 감시하는 가상의 운영 서비스 (AIMS 자체가 아님)

aims-collector
= structured log를 수집하여 service_log 테이블에 적재하는 역할

aims-core
= metric aggregation / detection / incident / AI / alert를 담당하는 AIMS 본체
```

**Phase 1 기준으로는 위 책임에 해당하는 실제 로직이 구현되어 있지 않습니다.** 세 서비스 모두 부팅 가능한 최소 골격 상태이며, 서비스 간 데이터 흐름(로그 생성 → 수집 → 적재 → 집계 → 탐지)은 아직 연결되어 있지 않습니다.

---

## 4. 현재 아키텍처

**현재 (Phase 1):**

```text
aims-demo
aims-collector
aims-core
PostgreSQL
```

4개 구성요소가 각각 독립적으로 존재하며, 서로 연결되어 동작하지는 않는 상태입니다.

**최종 목표:**

```text
aims-demo
    ↓
aims-collector
    ↓
PostgreSQL
    ↓
aims-core
    ├── Metric Aggregation
    ├── Rule Detection
    ├── Incident
    ├── AI Analysis
    └── Alert
```

---

## 5. DB Schema

`aims-core/src/main/resources/db/migration/V1__init_schema.sql`에서 다음 5개 테이블을 정의합니다.

| 테이블 | 설명 | 사용 예정 Phase |
|---|---|---|
| `service_log` | aims-demo가 남긴 구조화 로그가 저장되는 원본 테이블 | Phase 3 (collector가 적재), Phase 4 (집계 시 조회) |
| `metric_snapshot` | service_log를 시간 윈도우 단위로 집계한 지표(요청 수, 에러 수, P95/P99 지연 등) | Phase 4 (생성), Phase 5 (Rule 평가 시 조회) |
| `incident` | Rule Detection이 임계치 위반을 판단해 생성하는 장애 레코드와 상태(Lifecycle) | Phase 5 (생성), Phase 6 (조회 API), Phase 8 (Alert 발송) |
| `incident_signal` | 어떤 Rule/값이 Incident를 트리거했는지 기록하는 상세 근거 | Phase 5 (생성), Phase 7 (AI에 전달되는 구조화 입력) |
| `ai_analysis` | AI Analyzer가 생성한 요약/추정 원인/권장 조치 | Phase 7 (생성), Phase 6·8 (조회/알림에 활용) |

현재는 마이그레이션 스크립트만 작성되어 있으며, 이 테이블들에 실제로 데이터를 쓰거나 읽는 애플리케이션 로직은 아직 없습니다.

---

## 6. Phase Roadmap

```text
Phase 1  - 프로젝트/DB 기반 구성       [현재]
Phase 2  - aims-demo + structured log
Phase 3  - log collector
Phase 4  - metric aggregation
Phase 5  - rule detection + incident
Phase 6  - incident API
Phase 7  - AI analysis
Phase 8  - Slack alert
Phase 9  - Docker 통합
Phase 10 - AWS 배포
Phase 11 - E2E 검증 + 문서화
```

---

## 7. 검증 상태

개발 환경(회사 원격 PC)에서는 Docker Desktop을 사용할 수 없어, PostgreSQL은 **로컬에 직접 설치**해서 검증했습니다. Docker Compose 자체(`infra/docker-compose.yml`)는 별도로 검증이 필요합니다.

**검증 완료 (로컬 PostgreSQL 기준)**

- `aims-demo`: `./gradlew build` 성공, 실제 기동 후 3개 API + NORMAL/SLOW_DB/EXCEPTION 시나리오 + structured log 생성 확인
- `aims-core`: 로컬 PostgreSQL에 연결해 Flyway `V1`+`V2` migration 실제 적용, 5개 테이블 + `service_log.trace_id` UNIQUE 제약 생성 확인, `/actuator/health`의 `db` 컴포넌트 `UP` 확인
- `aims-collector`: 실제 로그 파일을 tail하여 PostgreSQL `service_log`에 실제 INSERT 확인, DB 장애 시 offset 미전진(재시도) 확인, 동일 배치 재처리 시 중복 미삽입 확인
- **End-to-end (EXCEPTION 시나리오 포함)**: `aims-demo` → `aims-collector` → PostgreSQL 전체 파이프라인을 NORMAL 요청과 EXCEPTION 시나리오 모두에 대해 실제로 실행해 `service_log`에 `status_code=500`, `exception_type=SimulatedFailureException` 행까지 정상 적재되는 것을 확인 (실제 캡처: 8절 "실행 결과 예시" 참고)

**미검증**

- `infra/docker-compose.yml`을 통한 PostgreSQL 기동 (Docker Desktop 필요 — Mac 등 Docker 가능 환경에서 검증 예정)

---

## 8. 실행 방법

### aims-demo (검증 완료)

```bash
cd aims-demo
./gradlew build
./gradlew bootRun
curl http://localhost:8080/actuator/health
```

### PostgreSQL + aims-core (Docker 환경, Mac 등 — 검증 예정)

```bash
cd infra
cp .env.example .env
docker compose up -d postgres        # 검증 예정 (Docker 환경 필요)

cd ../aims-core
./gradlew bootRun                    # 검증 예정 (PostgreSQL 필요, 기동 시 Flyway 자동 적용)
curl http://localhost:8081/actuator/health   # 검증 예정 (db 컴포넌트 UP 확인)

docker exec -it aims-postgres psql -U aims -d aims -c '\dt'   # 검증 예정 (5개 테이블 확인)
```

### 로컬 Windows 환경에서 Docker 없이 전체 파이프라인 실행하기 (검증 완료)

회사 원격 PC처럼 Docker를 쓸 수 없는 환경에서는, PostgreSQL을 로컬에 직접 설치해서 동일하게 테스트할 수 있습니다.

**0) 사전 준비 (한 번만)**

- PostgreSQL을 로컬에 설치하고 로그인 계정을 만든 뒤, `aims` 데이터베이스를 생성합니다.
  ```sql
  CREATE DATABASE aims;
  ```
- **JDK 17**이 필요합니다 (Spring Boot 3.3.4는 빌드 자체에 JDK 17 이상을 요구). 회사 PC처럼 다른 프로젝트가 Java 8을 쓰고 있어 전역 `JAVA_HOME`을 바꿀 수 없다면, JDK 17만 별도로 설치한 뒤 `aims-demo/`와 `aims-core/`에 각각 `gradle.properties`를 만들어 그 프로젝트에서만 사용하도록 지정합니다(전역 설정을 건드리지 않음, `.gitignore`에 등록되어 있어 각자 로컬에 직접 만들어야 함).
  ```properties
  # aims-demo/gradle.properties, aims-core/gradle.properties (각각 생성)
  org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-17.x.x-hotspot
  ```
- Python(3.10+)이 필요합니다. 설치 후:
  ```bash
  cd aims-collector
  pip install -r requirements-dev.txt
  ```

**1) DB 접속 정보를 로컬 전용 파일로 등록** (모두 `.gitignore`에 등록되어 있어 저장소에는 없음 — 각자 로컬에 직접 생성)

```yaml
# aims-core/src/main/resources/application-local.yml
spring:
  datasource:
    username: <본인 DB 계정>
    password: <본인 DB 비밀번호>
```

```powershell
# aims-collector/set_local_env.ps1
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "aims"
$env:DB_USER = "<본인 DB 계정>"
$env:DB_PASSWORD = "<본인 DB 비밀번호>"
```

**2) aims-core 실행 → Flyway migration 적용 확인**

```powershell
cd aims-core
$env:SPRING_PROFILES_ACTIVE = "local"
.\gradlew bootRun
```
`db: UP`이 나오면 성공:
```powershell
curl http://localhost:8081/actuator/health
```

**3) aims-demo 실행 (계속 켜둠)**

```powershell
cd aims-demo
.\gradlew bootRun
```

**4) 요청을 보내 structured log 생성**

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/passengers/checkin -Method Post -ContentType "application/json" -Body '{"flightNumber":"OZ1234","passengerName":"Hong Gildong"}'
Invoke-RestMethod -Uri http://localhost:8080/api/flights/OZ1234
Invoke-RestMethod -Uri http://localhost:8080/api/baggage -Method Post -ContentType "application/json" -Body '{"flightNumber":"OZ1234","weightKg":18.5}'
```

**5) aims-collector 실행 (계속 켜둠) → 로그를 PostgreSQL로 적재**

```powershell
cd aims-collector
. .\set_local_env.ps1
python -m aims_collector.main
```
`processed batch: N lines read, N valid, N inserted, offset -> ...` 로그가 뜨면 정상입니다.

**6) DB에서 결과 확인**

```powershell
psql -U <본인 DB 계정> -h localhost -d aims -c "SELECT trace_id, endpoint, status_code, response_time_ms FROM service_log ORDER BY id DESC LIMIT 10;"
```

**장애 시나리오까지 확인하려면** (창3이 계속 켜진 상태에서):
```powershell
Invoke-RestMethod -Uri http://localhost:8080/admin/scenario/exception -Method Post
Invoke-RestMethod -Uri http://localhost:8080/api/baggage -Method Post -ContentType "application/json" -Body '{"flightNumber":"OZ1234","weightKg":18.5}'
Invoke-RestMethod -Uri http://localhost:8080/admin/scenario/normal -Method Post
```
`status_code=500`, `exception_type=SimulatedFailureException`인 행이 `service_log`에 그대로 들어오는 것을 확인할 수 있습니다.

### 실행 결과 예시 (실제 캡처, EXCEPTION 시나리오 포함)

위 절차를 로컬 Windows 환경(Docker 없이, 로컬 PostgreSQL)에서 실제로 실행한 화면입니다. NORMAL 요청과 EXCEPTION 시나리오를 함께 재현했습니다.

**1) aims-demo — 구조화 로그 출력** (NORMAL 요청과 EXCEPTION 요청이 각각 INFO/ERROR로 기록됨)

![aims-demo 실행 화면](docs/screenshots/01-aims-demo.webp)

**2) aims-collector — 로그를 tail하여 PostgreSQL에 적재**

![aims-collector 실행 화면](docs/screenshots/02-aims-collector.webp)

**3) 요청 전송 — EXCEPTION 시나리오 전환 후 baggage 호출 시 실제 500 에러 발생**

![요청 전송 화면](docs/screenshots/03-request.png)

**4) DB 확인 — `status_code=500`인 행이 실제로 `service_log`에 적재됨**

![DB 확인 화면](docs/screenshots/04-db-check.webp)

### aims-collector 단독 테스트

```bash
cd aims-collector
pytest -v   # DB 연결이 안 되는 테스트 1개는 자동으로 skip됨
```

---

## 9. 기술 선택

현재 실제로 사용 중인 기술만 기록합니다.

- Java 17
- Spring Boot 3.3.4
- Gradle
- Python
- PostgreSQL
- Flyway
- Docker Compose

JPA, QueryDSL, Lombok, Redis, Kafka, Elasticsearch, Kubernetes, Terraform, pgvector 등은 아직 도입하지 않았으며, 필요성이 명확해지는 시점에 검토합니다.
