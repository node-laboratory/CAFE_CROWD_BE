# 카페붐빔 Backend — API 명세서 & 구현 계획

> **대상**: Kotlin + Spring Boot 백엔드
> **클라이언트**: Apps-in-Toss React Native 미니앱 (`@apps-in-toss/framework` SDK 2.x)
> **레포지토리**: 백엔드는 별도 레포 권장 (`cafe-crowd-backend`)
> **최종 갱신**: 2026-05-16

---

## 1. 목표

- 프런트의 `src/api/*.ts` 와 1:1로 정합한 REST API 제공
- 네이버 지도 관련 민감 API(검색·Geocoding 등)는 백엔드 프록시로 키 노출 최소화
- 카페 혼잡도 데이터는 운영자 도구 또는 외부 IoT로부터 수집해 `available` 플래그 갱신

## 2. 기술 스택

| 영역 | 채택 | 비고 |
|---|---|---|
| 언어 | **Kotlin 1.9+** | JDK 21 LTS |
| 프레임워크 | **Spring Boot 3.3.x** | Spring Web MVC (WebFlux 불필요) |
| DB | **PostgreSQL 16** | 지리 검색이 본격화되면 PostGIS 확장 |
| ORM | **Spring Data JPA + Hibernate** | 또는 jOOQ (선호 시) |
| 마이그레이션 | **Flyway** | `db/migration/V1__init.sql` |
| 빌드 | **Gradle Kotlin DSL** | `build.gradle.kts` |
| 외부 API 클라이언트 | **Spring WebClient** | Naver Search 호출 |
| 캐시 | **Caffeine (in-memory)** | v1은 메모리 캐시로 충분, 트래픽 ↑ 시 Redis |
| 로깅 | **Logback + JSON encoder** | 운영 환경에서 ELK/Loki 연동 용이 |
| 테스트 | **JUnit 5 + Kotest + MockK** | + Testcontainers (PostgreSQL) |
| 배포 | **Docker + Cloud Run / Fly.io / 코로케이션 서버** | v1은 단일 인스턴스 충분 |

## 3. 아키텍처

```
[RN 미니앱]
  ↓ HTTPS (JSON)
[Spring API Gateway = 단일 모듈]
  ├── CafeController
  ├── PlaceProxyController       → Naver Search (X-Naver-Client-Id/Secret)
  └── (선택) AdminController     → 운영 도구 (혼잡도 갱신)
       ↓
[PostgreSQL]
  └── cafes 테이블
```

- 단일 Spring Boot 어플리케이션. 마이크로서비스 분리 불필요.
- TLS는 앞단 로드밸런서(Cloud Run, Nginx 등)에서 종단.
- 클라이언트 → 백엔드는 **stateless HTTPS**. 세션/쿠키 없음, 인증 필요한 엔드포인트는 헤더 Bearer 토큰 사용 (운영자 도구만 해당).

## 4. 데이터 모델

### `cafes` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | `VARCHAR(40)` | PK | 슬러그 또는 UUID. 예: `cafe-1` |
| `name` | `VARCHAR(80)` | NOT NULL | |
| `available` | `BOOLEAN` | NOT NULL DEFAULT true | true=한산, false=만석 |
| `lat` | `DOUBLE PRECISION` | NOT NULL | WGS84 위도 |
| `lng` | `DOUBLE PRECISION` | NOT NULL | WGS84 경도 |
| `intro` | `TEXT` | NOT NULL DEFAULT '' | 소개문 |
| `address` | `VARCHAR(200)` | NOT NULL | 지번/구주소 |
| `thumbnail_url` | `VARCHAR(500)` | NOT NULL | |
| `photos` | `JSONB` | NOT NULL DEFAULT '[]' | `string[]` |
| `naver_place_url` | `VARCHAR(500)` | NOT NULL | 네이버 플레이스 딥링크 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | trigger로 자동 갱신 |

```sql
-- db/migration/V1__init.sql
CREATE TABLE cafes (
  id              VARCHAR(40)   PRIMARY KEY,
  name            VARCHAR(80)   NOT NULL,
  available       BOOLEAN       NOT NULL DEFAULT true,
  lat             DOUBLE PRECISION NOT NULL,
  lng             DOUBLE PRECISION NOT NULL,
  intro           TEXT          NOT NULL DEFAULT '',
  address         VARCHAR(200)  NOT NULL,
  thumbnail_url   VARCHAR(500)  NOT NULL,
  photos          JSONB         NOT NULL DEFAULT '[]'::jsonb,
  naver_place_url VARCHAR(500)  NOT NULL,
  created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_cafes_available ON cafes (available);
CREATE INDEX idx_cafes_location ON cafes (lat, lng);
-- PostGIS 도입 시: ALTER TABLE cafes ADD COLUMN geom geography(POINT, 4326);
```

## 5. 공통 사양

### 5.1 베이스 URL

| 환경 | URL |
|---|---|
| 로컬 개발 | `http://localhost:8080` |
| 스테이징 | `https://api-stg.cafecrowd.example.com` |
| 운영 | `https://api.cafecrowd.example.com` |

프런트는 `API_BASE_URL` 환경변수로 주입 (`src/api/client.ts`).

### 5.2 요청 헤더

```
Content-Type: application/json
Accept: application/json
User-Agent: cafecrowd-rn/<version>   (선택)
```

운영자 도구 엔드포인트:
```
Authorization: Bearer <admin-jwt>
```

### 5.3 에러 응답 표준

모든 4xx/5xx는 다음 shape:

```json
{
  "code": "CAFE_NOT_FOUND",
  "message": "사람이 읽을 한국어 메시지",
  "details": { "id": "cafe-9" }
}
```

| HTTP | code 예시 | 사용 시점 |
|---|---|---|
| 400 | `BAD_REQUEST`, `INVALID_QUERY` | 파라미터 검증 실패 |
| 401 | `UNAUTHORIZED` | 관리자 토큰 없거나 만료 |
| 403 | `FORBIDDEN` | 권한 부족 |
| 404 | `CAFE_NOT_FOUND` | 리소스 없음 |
| 429 | `RATE_LIMIT` | 호출 한도 초과 (Naver 프록시 등) |
| 500 | `INTERNAL_ERROR` | 서버 예외 |
| 502 | `UPSTREAM_ERROR` | Naver API 실패 |

`@RestControllerAdvice`로 일괄 매핑.

### 5.4 CORS

RN 환경(WebView 외부)에선 CORS preflight가 보통 발생하지 않지만, WebView에서 `fetch`할 수도 있으므로 안전하게:

```kotlin
@Configuration
class CorsConfig : WebMvcConfigurer {
  override fun addCorsMappings(reg: CorsRegistry) {
    reg.addMapping("/api/**")
      .allowedOrigins("https://localhost", "intoss://nodelab-cafe")
      .allowedMethods("GET", "POST")
      .allowedHeaders("*")
  }
}
```

---

## 6. API 엔드포인트

### 6.1 `GET /api/cafes` — 카페 전체 목록

**Request**

| 쿼리 | 타입 | 필수 | 기본 | 설명 |
|---|---|---|---|---|
| `bbox` | `string` | X | 없음 | `swLat,swLng,neLat,neLng` 형태로 지도 가시 영역 필터링 (선택. v1에서는 무시) |

**Response 200**

```json
[
  {
    "id": "cafe-1",
    "name": "국민카페",
    "available": true,
    "lat": 37.5409,
    "lng": 127.0696,
    "intro": "주소 or 사장님한테 소개 한마디 받기",
    "address": "서울 광진구 광장동 111-22",
    "thumbnailUrl": "https://cdn.cafecrowd.example.com/cafes/cafe-1/thumb.webp",
    "photos": [
      "https://cdn.cafecrowd.example.com/cafes/cafe-1/1.webp",
      "https://cdn.cafecrowd.example.com/cafes/cafe-1/2.webp"
    ],
    "naverPlaceUrl": "https://map.naver.com/v5/search/국민카페"
  }
]
```

**캐시**: `Cache-Control: public, max-age=60` (브라우저 측 최대 1분 캐시).
서버 측에서도 동일 응답을 60초 Caffeine 캐시.

**Kotlin 컨트롤러 시그니처**

```kotlin
@RestController
@RequestMapping("/api/cafes")
class CafeController(private val service: CafeService) {

  @GetMapping
  fun list(): List<CafeDto> = service.findAll()

  @GetMapping("/{id}")
  fun get(@PathVariable id: String): CafeDto =
    service.findById(id) ?: throw CafeNotFoundException(id)
}
```

```kotlin
data class CafeDto(
  val id: String,
  val name: String,
  val available: Boolean,
  val lat: Double,
  val lng: Double,
  val intro: String,
  val address: String,
  val thumbnailUrl: String,
  val photos: List<String>,
  val naverPlaceUrl: String,
)
```

> JPA 엔티티 ↔ DTO 매핑: MapStruct 또는 수동 toDto() 확장 함수.

### 6.2 `GET /api/cafes/{id}` — 단일 카페 조회

> 현재 프런트는 호출 안 함. v1.1에서 카페 상세 단독 페이지 도입 시 필요.

**Response 200**: `CafeDto` 단일
**Response 404**: `{ code: "CAFE_NOT_FOUND", ... }`

### 6.3 `GET /api/places/search` — 장소 검색 프록시 (v1.1)

네이버 Local Search API를 백엔드에서 호출해 결과를 정규화. **NCP `ncpKeyId`와 별개 키 사용** ([developers.naver.com](https://developers.naver.com)에서 발급).

**Request**

| 쿼리 | 타입 | 필수 | 기본 | 설명 |
|---|---|---|---|---|
| `q` | `string` | O | — | 검색어 (UTF-8) |
| `limit` | `int` | X | 5 | 1~10 |

**Response 200**

```json
{
  "items": [
    {
      "name": "스타벅스 광장점",
      "category": "카페,디저트>커피전문점",
      "address": "서울특별시 광진구 광장동 ...",
      "roadAddress": "서울특별시 광진구 광나루로 ...",
      "phone": "02-1234-5678",
      "lat": 37.5409,
      "lng": 127.0696
    }
  ]
}
```

**Response 502**: 네이버 응답 실패. `{ code: "UPSTREAM_ERROR", ... }`

**구현 포인트**

1. WebClient로 호출:
   ```
   GET https://openapi.naver.com/v1/search/local.json?query={q}&display={limit}
   Headers: X-Naver-Client-Id, X-Naver-Client-Secret
   ```
2. 응답의 `mapx`, `mapy`는 **KATEC TM128** 좌표. WGS84로 변환 필요.
3. `title` 필드는 HTML 태그(`<b>`)가 섞여 있으므로 strip 후 노출.
4. Caffeine에 60초 캐시 (동일 쿼리 폭주 방지).
5. 일일 호출 한도 25,000건 → 사용량 모니터링 권장.

**좌표 변환 코드 스니펫**

```kotlin
// TM128 → WGS84 (네이버 공식 변환식)
fun tm128ToWgs84(x: Double, y: Double): Pair<Double, Double> {
  // 외부 라이브러리 권장: proj4j-epsg
  // 의존성: implementation("org.locationtech.proj4j:proj4j-epsg:1.3.0")
  val crsFactory = CRSFactory()
  val source = crsFactory.createFromName("EPSG:5179")    // 또는 네이버 자체 TM128
  val target = crsFactory.createFromName("EPSG:4326")
  val transform = CoordinateTransformFactory().createTransform(source, target)
  val out = ProjCoordinate()
  transform.transform(ProjCoordinate(x, y), out)
  return out.y to out.x   // lat, lng
}
```

> 주의: 네이버 mapx/mapy는 표준 EPSG:5179가 아니라 자체 변형이라 정확한 결과를 위해 `naver-map-coord-converter` 같은 검증된 라이브러리 사용 권장. 또는 [네이버 공식 문서의 변환식](https://navermaps.github.io/maps.js.ncp/docs/tutorial-1-Coordinate-System.example.html) 직접 구현.

### 6.4 `PATCH /api/admin/cafes/{id}/availability` — 혼잡도 갱신 (관리자)

**Request**

```http
PATCH /api/admin/cafes/cafe-1/availability
Authorization: Bearer <admin-jwt>
Content-Type: application/json

{
  "available": false
}
```

**Response 200**: 갱신된 `CafeDto`

**구현 포인트**

- 인증: 운영자만. JWT 또는 Basic auth (v1은 Basic도 충분, HTTPS 위에서)
- `updated_at` 자동 갱신 (트리거 또는 `@PreUpdate`)
- IoT 자동 갱신을 고려한다면 이 엔드포인트를 Bot/Worker가 호출

---

## 7. 보안

### 7.1 비밀 정보 보관

`application.yml` (절대 git 커밋 X):

```yaml
naver:
  search:
    client-id: ${NAVER_SEARCH_CLIENT_ID}
    client-secret: ${NAVER_SEARCH_CLIENT_SECRET}
admin:
  bearer-token: ${ADMIN_BEARER_TOKEN}
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

- 환경변수 주입은 배포 플랫폼의 Secret 매니저 사용 (Cloud Run Secret, GitHub Actions Secrets, .env)
- 로컬 개발용 `.env`는 gitignore

### 7.2 키 노출 영역

| 키 | 보관 위치 | 노출 위험 |
|---|---|---|
| **NCP `ncpKeyId`** (지도 SDK) | 클라이언트 번들 | 노출 불가피. Referer 화이트리스트 + 일일 한도로 방어 |
| **Naver Search Client Secret** | 백엔드 환경변수 | 0 — 외부 노출 절대 금지 |
| **관리자 토큰** | 백엔드 환경변수 + 운영자 도구만 | 0 |
| **DB 비밀번호** | 백엔드 환경변수 | 0 |

### 7.3 Rate Limit (선택)

`/api/places/search`에 대해서는 IP당 분당 30회 정도 제한 권장. `Bucket4j` 또는 Spring Cloud Gateway 사용.

### 7.4 HTTPS

라이브 환경은 무조건 TLS. Cloud Run 사용 시 자동 제공. 자체 호스팅이면 Caddy 또는 Nginx + Let's Encrypt.

---

## 8. 설정 / 환경 변수

| 변수 | 예시 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://...:5432/cafecrowd` | |
| `DB_USER` | `cafecrowd` | |
| `DB_PASSWORD` | `***` | |
| `NAVER_SEARCH_CLIENT_ID` | `1q2w3e4r5t` | developers.naver.com 발급 |
| `NAVER_SEARCH_CLIENT_SECRET` | `***` | |
| `ADMIN_BEARER_TOKEN` | `***` | 운영자 도구용 |
| `SERVER_PORT` | `8080` | |
| `SPRING_PROFILES_ACTIVE` | `prod` | local/stg/prod |

---

## 9. 프런트 측 매핑

### 9.1 환경변수 주입

프런트 빌드 시 `API_BASE_URL`을 주입해야 함:

**옵션 A — `@granite-js/plugin-env` 사용**

`granite.config.ts`에 추가:
```ts
import { env } from '@granite-js/plugin-env';

plugins: [
  router({ watch: true }),
  env({
    API_BASE_URL: process.env.API_BASE_URL ?? '',
  }),
  appsInToss({ ... }),
],
```

`src/api/client.ts`에서:
```ts
export const API_BASE_URL: string = import.meta.env.API_BASE_URL || '';
```

**옵션 B — 빌드 시 치환 스크립트**

배포 직전에 `src/api/client.ts` 안의 `''` 자리에 실제 URL을 sed로 주입.

### 9.2 프런트가 호출하는 엔드포인트 (현 시점)

```
GET /api/cafes                     ← src/api/cafes.ts#fetchCafes
```

향후 추가 예정:
```
GET /api/places/search?q=...       ← 장소 검색 도입 시
GET /api/cafes/{id}                ← 카페 단독 페이지 도입 시
```

---

## 10. 단계별 구현 로드맵

### Phase 1 — MVP (Week 1)
- [ ] Spring Boot 프로젝트 초기화 (Gradle Kotlin DSL, JDK 21)
- [ ] Flyway + PostgreSQL 셋업, `cafes` 테이블 생성
- [ ] 5개 mockData 그대로 INSERT (`V1__seed.sql`)
- [ ] `GET /api/cafes` 구현 + 통합 테스트
- [ ] 에러 핸들러 (`@RestControllerAdvice`)
- [ ] Dockerfile + Cloud Run 또는 Fly.io 배포
- [ ] 프런트에 `API_BASE_URL` 주입 (mockData → 실 API 전환 확인)

**완료 기준:** 프런트 RN 미니앱이 백엔드에서 5개 카페 받아 지도에 표시

### Phase 2 — 관리자 도구 (Week 2)
- [ ] `PATCH /api/admin/cafes/{id}/availability` 구현
- [ ] Basic auth 또는 단순 Bearer 토큰
- [ ] 간단한 관리자용 정적 페이지 (Thymeleaf 또는 별도 admin UI)
- [ ] 카페 CRUD (POST/PUT/DELETE) — 게스트 노출 X, 관리자만

### Phase 3 — 장소 검색 (Week 3)
- [ ] developers.naver.com에서 Application 등록, Client ID/Secret 발급
- [ ] `GET /api/places/search` 프록시 구현
- [ ] TM128 → WGS84 좌표 변환
- [ ] Caffeine 캐시 + Bucket4j rate limit
- [ ] 프런트에 `src/api/places.ts` 추가, 검색바에서 호출
- [ ] WebView 내 `naver.maps.Service.geocode` 호출 제거

### Phase 4 — 운영화 (Week 4+)
- [ ] 모니터링: Prometheus + Grafana 또는 Cloud Run 기본 메트릭
- [ ] 로그 수집: 구조화 JSON 로그 + ELK/Loki
- [ ] 알림: 5xx 비율, 응답 시간 P95, NCP 일일 한도 도달
- [ ] 백업: PostgreSQL 일일 덤프 → S3 호환 스토리지

---

## 11. 폴더 구조 권장

```
cafe-crowd-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── .env.example
├── src/
│   ├── main/
│   │   ├── kotlin/com/cafecrowd/
│   │   │   ├── CafeCrowdApplication.kt
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.kt
│   │   │   │   ├── WebClientConfig.kt
│   │   │   │   └── SecurityConfig.kt
│   │   │   ├── cafe/
│   │   │   │   ├── CafeController.kt
│   │   │   │   ├── CafeService.kt
│   │   │   │   ├── CafeRepository.kt
│   │   │   │   ├── CafeEntity.kt
│   │   │   │   └── CafeDto.kt
│   │   │   ├── place/
│   │   │   │   ├── PlaceProxyController.kt
│   │   │   │   ├── PlaceService.kt
│   │   │   │   ├── NaverSearchClient.kt
│   │   │   │   └── TM128Converter.kt
│   │   │   ├── admin/
│   │   │   │   └── AdminCafeController.kt
│   │   │   └── common/
│   │   │       ├── GlobalExceptionHandler.kt
│   │   │       ├── ErrorResponse.kt
│   │   │       └── DomainException.kt
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__init.sql
│   │           └── V2__seed.sql
│   └── test/
│       └── kotlin/com/cafecrowd/
│           ├── cafe/CafeControllerTest.kt
│           └── place/PlaceProxyControllerTest.kt
```

---

## 12. OpenAPI 스펙 자동화 (선택)

`springdoc-openapi-starter-webmvc-ui` 추가:

```kotlin
dependencies {
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
```

- `/swagger-ui.html` 에서 인터랙티브 문서 자동 생성
- 프런트 개발자가 DTO 변경 즉시 확인 가능
- 운영에서는 swagger UI 비활성화 + JSON spec만 보존

---

## 13. 체크리스트 (Phase 1 출시 직전)

- [ ] `GET /api/cafes` 응답 shape이 `src/api/cafes.ts` `CafeDto`와 정확히 일치
- [ ] `available` 필드가 boolean인지 (string `"true"` 등 금지)
- [ ] `photos`가 항상 배열 (null 금지)
- [ ] `naverPlaceUrl` 형식이 RN의 `Linking.openURL`로 열 수 있는 https URL
- [ ] 응답 시간 P95 < 300ms
- [ ] DB 연결 풀 사이즈 적정 (HikariCP 기본 10 충분)
- [ ] Health check 엔드포인트 `GET /actuator/health`
- [ ] CORS 설정으로 `https://localhost` (WebView) 허용
- [ ] HTTPS 종단 확인
- [ ] DB 백업 스크립트 동작 확인

---

## 14. 자주 헷갈리는 포인트

### Q. 프런트 `CafeDto`와 백엔드 `CafeDto`가 분리되어 있는데 어떻게 동기화?

OpenAPI 스펙 자동 생성 → 프런트에서 `openapi-typescript`로 타입 자동 생성 가능. v1에서는 수동 동기화로 충분.

### Q. 폴링 주기 1분인데 백엔드 부하 괜찮나?

사용자 100명 × 1회/분 = 100 req/min = 1.67 req/s. PostgreSQL 0.1ms 쿼리 + 60초 캐시면 거의 0 부하. 사용자 10,000명까지는 단일 인스턴스로 충분.

### Q. 변경분만 받는 delta sync 안 함?

프런트와 합의: v1은 전체 GET. 카페 수 500개 넘어가는 시점에 `GET /api/cafes/availability` (id+available 만 반환) 가벼운 엔드포인트 추가하는 것으로 합의.

### Q. 인증 없이 카페 목록을 노출해도 되나?

공개 정보이므로 OK. 단, 관리자 도구는 반드시 인증 필요.

### Q. Docker 이미지 크기

`amazoncorretto:21-alpine` 베이스 + JLink 또는 Native Image 사용 시 100~200MB 가능. 일반 JVM 빌드는 300~500MB.

---

## 15. 참고 링크

- [Apps-in-Toss Developer Center](https://developers-apps-in-toss.toss.im/)
- [Naver Cloud Platform — Maps](https://www.ncloud.com/product/applicationService/maps)
- [Naver Developers — Search API](https://developers.naver.com/docs/serviceapi/search/local/local.md)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
