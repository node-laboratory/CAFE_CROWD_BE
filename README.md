# cafe-crowd-backend

[Apps-in-Toss](https://developers-apps-in-toss.toss.im) "카페붐빔" RN 미니앱의 백엔드.
Kotlin + Spring Boot 4 기반 REST API로 카페 목록·혼잡도 갱신·네이버 장소 검색 프록시를 제공한다.

## 주요 기능

- **카페 목록/조회** — JSONB로 photos 저장, 60초 Caffeine 캐시
- **혼잡도 갱신** — 운영자용 Bearer 인증 PATCH 엔드포인트
- **네이버 장소 검색 프록시** — Client Secret 백엔드 보호, IP당 분당 30회 Rate Limit
- **운영화** — JSON 로깅, Prometheus 메트릭, RequestId MDC, 글로벌 에러 응답

## 기술 스택

Kotlin 2.2 · Spring Boot 4.0 · JDK 21 · PostgreSQL 16 · Flyway · JPA(Hibernate) · Caffeine · Bucket4j · Micrometer/Prometheus · springdoc OpenAPI · Testcontainers · Gradle Kotlin DSL

## 빠른 시작 (로컬)

### 1. 사전 요구

- JDK 21 
- Docker

### 2. DB 띄우기

```bash
docker run -d --name cafecrowd-pg \
  -e POSTGRES_USER=cafecrowd \
  -e POSTGRES_PASSWORD=cafecrowd \
  -e POSTGRES_DB=cafecrowd \
  -p 5432:5432 \
  postgres:16-alpine
```

### 3. 환경변수

프로젝트 루트에 `.env` 작성 (`.env.example` 참고):

```bash
DB_URL=jdbc:postgresql://localhost:5432/cafecrowd
DB_USER=cafecrowd
DB_PASSWORD=cafecrowd
NAVER_SEARCH_CLIENT_ID=
NAVER_SEARCH_CLIENT_SECRET=
ADMIN_BEARER_TOKEN=local-dev-token
```

`spring-dotenv`가 자동 로드함. 별도 export 불필요.

### 4. 실행

```bash
./gradlew bootRun
```

부팅 후:
- API: `http://localhost:8080/api/cafes`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/prometheus`

### 5. 동작 확인

```bash
# 카페 목록 (시드 5건)
curl http://localhost:8080/api/cafes | jq .

# Admin 인증 — 토큰 일치 시 200
curl -X PATCH http://localhost:8080/api/admin/cafes/cafe-1/availability \
  -H "Authorization: Bearer local-dev-token" \
  -H "Content-Type: application/json" \
  -d '{"available":false}'

# 네이버 검색 (NAVER_SEARCH_CLIENT_ID/SECRET 필요)
curl "http://localhost:8080/api/places/search?q=스타벅스&limit=3"
```

## API 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/cafes` | 공개 | 카페 전체 목록 (60초 캐시) |
| GET | `/api/cafes/{id}` | 공개 | 단일 카페 조회 |
| PATCH | `/api/admin/cafes/{id}/availability` | Bearer | 혼잡도 갱신 |
| GET | `/api/places/search?q=&limit=` | 공개 (Rate Limit) | 네이버 장소 검색 프록시 |

## 프로젝트 구조

```
src/main/kotlin/com/cafecrowd/
├── cafe/      카페 도메인 (Controller/Service/Repository/Entity/Dto)
├── admin/     운영자 도구 (AuthFilter + Controller)
├── place/     네이버 검색 프록시 + TM128 변환 + Rate Limit
├── common/    ErrorResponse, GlobalExceptionHandler, RequestIdFilter
└── config/    Cors / Cache / OpenAPI

src/main/resources/
├── application*.yaml          프로필별 설정
├── logback-spring.xml         local: 콘솔 / prod: JSON
└── db/migration/V*.sql        Flyway
```

## 테스트

```bash
./gradlew test           # 전체 (Testcontainers 포함, Docker 필요)
./gradlew compileKotlin  # 컴파일만
```

- 슬라이스 테스트 (`@WebMvcTest`): 7건
- 통합 테스트 (Testcontainers PostgreSQL): 5건
- 총 12건

## 빌드 & 배포

### 로컬 빌드

```bash
./gradlew bootJar           # build/libs/*.jar
docker build -t cafe-crowd-be:local .
```

### 운영 (ECR + EC2 + RDS)

```bash
# EC2의 .env에 APP_IMAGE=<ECR URI>:<tag>, DB_URL=<RDS endpoint> 등 설정
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <ECR URI>
docker compose pull app
docker compose up -d --no-build
```

배포 인프라 결정 배경은 [docs/wiki/Decisions-and-FAQ.md](docs/wiki/trouble-shooting-1.md) Case 6, 7 참조.

