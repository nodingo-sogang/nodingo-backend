# nodingo-backend

AI융합캡스톤디자인 백엔드 서버

---

## 🛠️ 1. 기술 스택 (Tech Stacks)

### Core Framework & Runtime

* **Language / Runtime:** Java 21 (Toolchain OpenJDK 21)
* **Framework:** Spring Boot 3.4.5
* **Dependency Management:** Spring Dependency Management 1.1.7

### Persistence & Data Infrastructure

* **Primary Database:** PostgreSQL (pgvector 확장 - 1536차원 벡터 인덱싱)
* **NoSQL / Cache / Real-time Structure:** Redis (Spring Boot Starter Data Redis)
* **ORM:** Spring Data JPA (Hibernate 6.x)
* **Type-Safe Query Builders:** QueryDSL JPA 5.0.0 (Jakarta Spec 표준 규격 적용)

### Security & Authentication

* **Security Framework:** Spring Boot Starter Security
* **Protocol & Client:** Spring Boot Starter OAuth2 Client (Naver OAuth2)
* **Token Standard:** JSON Web Token (JJWT API / Impl / Jackson 0.13.0)

### Integration & Distributed Communication

* **HTTP Client / Microservices:** Spring Cloud Starter OpenFeign (BOM 2024.0.2)
* **Push Notification:** Google Firebase Admin SDK 9.2.0 (인앱 FCM 알림 허브)
* **Data Parsing / Web Scraping:** Jsoup 1.17.2

### 🕒 Batch & Automation Engine

* **Batch Framework:** Spring Boot Starter Batch
* **Batch Integration:** Spring Batch Integration

### Documentation & Utilities

* **API Open Specifications:** Springdoc OpenAPI Starter WebMVC UI 2.8.14 (Swagger UI)
* **Boilerplate Reducer:** Lombok
* **Data Validation Guard:** Spring Boot Starter Validation

---

## ⚡ 2. 핵심 아키텍처 및 기능

### 🤖 A. Spring Batch & FastAPI 연동 실시간 AI 뉴스 분석 파이프라인 (핵심 엔진)

매일 오전 5시, `NewsScheduler`가 `dailyNewsJob`을 트리거하며, 총 **4개의 Step**이 순차 실행됩니다.

---

#### Step 1. `newsStep` — 뉴스 수집 + AI 분석 + 지식 그래프 구조화 (`NewsAiWriter`)

Spring Batch의 청크 지향 처리(Chunk Size: 10) 방식으로 수집된 뉴스를 10개씩 묶어 아래 처리를 원자적으로 수행합니다.

1. **1차 DB 저장:** 수집된 뉴스 엔티티를 먼저 영속화하여 ID를 확보합니다.
2. **AI 분석 요청:** 저장된 뉴스와 오늘 날짜 기준 기존 키워드(word, normalizedWord, embedding) 목록을 `NewsBatch.Request` JSON 래퍼로 묶어 FastAPI AI 서버(`aiClient.analyzeNewsBatch`)로 전송합니다.
3. **뉴스 임베딩/요약 업데이트:** AI 응답으로 받은 벡터 임베딩과 요약 본문을 뉴스 엔티티에 반영합니다.
4. **키워드 계층 구조화 (N+1 방지 캐싱):**
    - 청크 처리 전 `specificCache`, `macroCache`를 날짜 기준 한 번에 조회하여 로컬 맵으로 보유합니다.
    - 대분류(Persona) → 중분류(Macro) → 소분류(Specific) 키워드를 `computeIfAbsent`로 신규 생성 또는 기존 매핑하여 뉴스-키워드 관계(weight 포함)를 저장합니다.
5. **`KeywordRelation` 그래프 구축 (Upsert):**
    - AI가 반환한 `keyword_relations`를 `subjectNormalizedWord` / `relatedNormalizedWord` 기준으로 `specificCache`에서 직접 키워드를 매핑합니다 (AI의 keyword_id null 방어).
    - 두 키워드 ID를 오름차순으로 정렬하여 `findByPair`로 중복 존재 여부를 확인합니다.
    - 이미 존재하는 relation은 `updateRelation(score)`로 점수만 갱신하고, 없으면 신규 저장합니다.
    - 청크별 배치 로그: `AI total / toSave / skippedNullNorm / skippedNotFound / duplicateUpdated`

---

#### Step 2. `relationStep` — 뉴스 간 관계 생성 (`newsRelationTasklet`)

뉴스 임베딩 벡터 기반으로 오늘 수집된 뉴스들 사이의 유사도를 계산하고 `NewsRelation` 테이블에 적재합니다.

---

#### Step 3. `recommendStep` — 사용자별 추천 키워드 생성

* 온보딩 완료 유저 전체를 청크(Chunk Size: 30) 단위로 로드합니다.
* 각 유저의 관심사 임베딩과 오늘의 키워드 벡터를 코사인 유사도로 비교하여 개인화 추천 키워드(`RecommendKeyword`)를 생성/갱신합니다.

---

#### Step 4. `recommendSummaryStep` — 추천 키워드 요약 + 퀴즈 생성 (병렬 처리)

* `SynchronizedItemStreamReader` + `ThreadPoolTaskExecutor`(`batchQuizExecutor`) 조합으로 멀티스레드 병렬 처리합니다.
* summary가 없는 추천 키워드만 대상으로 상위 뉴스 3건을 묶어 FastAPI `summarizeKeywords`에 요청합니다.
* 요약 결과를 `RecommendKeyword.summary`에 저장하고, 동시에 해당 키워드에 퀴즈가 없으면 `QuizGenerationService`로 퀴즈를 인라인 생성합니다.
* 관련 뉴스가 부족한 경우 "관련 뉴스가 부족하여 요약할 수 없습니다." 메시지로 폴백 처리합니다.

#### Step 5. `neighborKeywordQuizStep` — 이웃 키워드 퀴즈 병렬 생성 (`NeighborKeywordQuizTasklet`)

* 추천 키워드는 아니지만 뉴스가 연결된 **이웃 키워드**(그래프 엣지로 등장하는 소분류)를 대상으로 합니다.
* `CompletableFuture.runAsync` + `batchQuizExecutor`로 키워드별 요약/퀴즈 생성을 병렬 수행하고 `CompletableFuture.allOf(...).join()`으로 전체 완료를 동기화합니다.
* 이미 퀴즈가 있는 키워드는 스킵하여 중복 생성을 방지합니다.

---

### 🚀 B. 온보딩 비동기 임베딩 초기화

유저 최초 온보딩 완료 시 즉시 201 응답을 반환하고, 무거운 AI 연산(유저 임베딩 초기화 + 추천 키워드 생성)은 `OnboardingAsyncService`를 통해 비동기로 처리합니다. `AsyncConfig`에서 `ThreadPoolTaskExecutor`(corePool=5, maxPool=10)를 구성합니다.

---

### 📊 C. Redis 활용 (리더보드 + 토큰 블랙리스트 + 그래프 캐싱)

#### C-1. `ZSET`(Sorted Set) 기반 실시간 주간 리더보드 엔진

* 유저가 퀴즈 정답, 노드 탐험, 키워드 스크랩, 출석체크 등의 게임 액션을 달성할 때마다 Redis `ZSET` 명령어로 O(log N) 효율로 즉각 업데이트합니다.
* `scope == PERSONA`: 관심 분야별 리더보드를 Redis에서 직접 추출하여 TOP 10 바인딩합니다.
* `scope == FRIENDS`: 친구 네트워크 ID 목록과 결합하여 Java 메모리 단에서 정렬 컷 연산을 수행합니다.
* `reverseRank` 연산으로 10위권 밖에서도 내 등수를 하단 고정 UI에 O(1)로 바인딩합니다.

#### C-2. JWT 토큰 블랙리스트 (로그아웃 처리 + TTL)

* 로그아웃 시 `AuthCommandService`가 해당 Access Token을 `blacklist:{token}` 키로 Redis에 등록합니다.
* 이때 토큰의 **잔여 만료 시간(remainingMillis)을 TTL로 설정**하여, 토큰이 자연 만료되는 시점에 Redis 키도 자동 소멸하므로 별도의 정리 배치가 필요 없습니다.
* 토큰 재발급(`reissue`) 요청 시 `hasKey`로 블랙리스트 여부를 검사하여 이미 로그아웃된 토큰의 재사용을 차단합니다.

#### C-3. 그래프 미리보기 캐싱

* `@Cacheable(value = "batch:graph")`로 유저별/중심키워드별 그래프 레이아웃을 캐싱하며, 배치 완료 시 `MyJobListener`가 전체 캐시를 삭제하여 최신 데이터를 반영합니다.

---

### 🗺️ D. 이슈 맵 그래프 시각화

* `GraphQueryService`는 유저의 추천 키워드와 `KeywordRelation` 데이터를 결합하여 FastAPI에 그래프 레이아웃 계산을 요청합니다.
* 추천 키워드에 없는 relation 연결 노드도 실제 `Keyword` 엔티티에서 word/persona를 가져오며, 기본 score 0.45를 부여하여 Unknown 필터링을 방지합니다.
* 결과는 `@Cacheable(value = "batch:graph")`로 Redis 캐싱되며, 배치 완료 시 자동으로 전체 삭제됩니다.

### 🛡️ E. AOP 기반 온보딩 완료 가드

* `@RequireOnboardingCompleted` 커스텀 어노테이션을 메서드 또는 클래스 단위로 선언하면, `OnboardingStatusAspect`가 `@Around` 어드바이스로 해당 요청을 가로챕니다.
* 컨트롤러 인자의 `CustomOAuth2User`에서 온보딩 완료 여부를 검사하고, 미완료 시 `OnboardingNotCompletedException`을 던져 비즈니스 로직 진입 전에 차단합니다.
* 온보딩이 선행되어야 하는 그래프/추천/퀴즈 API에 공통 적용하여 컨트롤러마다 중복 검증 코드를 제거합니다.

---

## 🗺️ 3. 백엔드 API 명세 (Domain Matrix)

### 🔐 1. Auth (인증 및 자격 증명)

* `POST /api/auth/logout` : 로그아웃 처리 및 리프레시 토큰 폐기
* `POST /api/auth/refresh` : 토큰 재발급 (Refresh Token Exchange)
* `DELETE /api/auth/withdraw` : 회원 탈퇴 (네이버 연동 해제 및 계정 전체 데이터 영구 삭제)

### 🛠️ 2. Admin - Batch (관리자용 배치 실행 API)

* `POST /api/batch/news-collect` : 뉴스 수집 배치 수동 실행
* `GET /api/batch/news-request-spec` : [파이썬 협업용] 뉴스 분석 요청 JSON 스펙 조회
* `POST /api/batch/notification-push` : 시간당 알림 배치 수동 실행

### 👥 3. Friendship (친구 관계 API)

* `GET /api/friends` : 내 친구 목록 조회
* `POST /api/friends/request` : 친구 요청 보내기
* `POST /api/friends/accept` : 친구 요청 수락하기
* `GET /api/friends/received` : 나에게 온 요청 목록 조회

### 🌐 4. Graph (이슈 맵 그래프 시각화 API)

* `GET /api/graphs/tabs` : 오늘의 추천 탭 목록 조회
* `GET /api/graphs/nodes` : 특정 탭 기준 그래프 관계도 조회 (keywordId 없으면 전체 보기)
* `GET /api/graphs/nodes/{nodeId}/summaries` : 특정 노드 상세 요약 조회
* `POST /api/graphs/nodes/{keywordId}/explore` : 노드 탐험 기록 저장 및 XP 적재

### 🔖 5. Keyword Scrap (키워드 스크랩 API)

* `POST /api/keywords/{keywordId}/scrap` : 키워드 요약 스크랩 추가 및 XP 적재
* `DELETE /api/keywords/{keywordId}/scrap` : 키워드 요약 스크랩 취소 및 XP 차감
* `GET /api/users/scraps/keywords/nodes` : 스크랩한 키워드 노드 목록 조회 (그래프용, 20개씩 Slice)
* `GET /api/users/scraps/keywords` : 스크랩한 키워드 요약 목록 조회 (목록용, 4개씩 Slice)

### 📰 6. News (뉴스 API)

* `GET /api/news/{newsId}` : 뉴스 상세 조회

### 📦 7. News Scrap (뉴스 스크랩 API)

* `POST /api/news/{newsId}/scrap` : 뉴스 스크랩 추가
* `DELETE /api/news/{newsId}/scrap` : 뉴스 스크랩 취소

### 🔔 8. Notification (알림 API)

* `GET /api/users/notifications` : 유저 알림 설정 조회
* `PATCH /api/users/notifications/time` : 유저 알림 시간 설정
* `PATCH /api/users/notifications/token` : FCM 토큰 갱신
* `POST /api/users/notifications/test` : FCM 단건 전송 테스트

### 🎯 9. Quiz (퀴즈 API)

* `GET /api/graphs/nodes/{keywordId}/quizzes` : 퀴즈 목록 조회
* `POST /api/graphs/nodes/{keywordId}/quizzes/{quizId}/submit` : 퀴즈 정답 제출 및 XP 적재

### 👤 10. User (사용자 API)

* `GET /api/users/onboarding/status` : 온보딩 상태 조회
* `POST /api/users/onboarding` : 온보딩 관심사 설정 (응답 201 즉시 반환, AI 초기화 비동기 처리)
* `GET /api/users/game` : 내 게임 프로필(레벨, XP) 조회
* `GET /api/users/progress` : 내 탐험 진행률 조회 및 출석 체크 XP 연동
* `GET /api/users/ranking` : 주간 랭킹 TOP 10 및 내 등수 조회
* `GET /api/users/badges` : 내 뱃지 목록 조회
* `GET /api/users/keywords/macro` : 중분류(Macro) 목록 조회
* `GET /api/users/keywords/personas` : 대분류(Persona) 목록 조회
* `GET /api/users/keywords/specific` : 소분류(Specific) 목록 조회
* `GET /api/users/search` : 닉네임 유저 검색

---

## 🕒 4. Cron Scheduler Engine

### A. NewsScheduler

* **Cron:** `0 0 5 * * *` (Asia/Seoul)
* **설명:** 매일 오전 5시에 `dailyNewsJob`을 자동 트리거합니다. 새벽 5시 이전 요청은 전날(`targetDate.minusDays(1)`)로 보정됩니다.

### B. NotificationScheduler

* **Cron:** `0 0 * * * *`
* **설명:** 매 시간 정각에 `hourlyNotificationJob`을 구동하여 FCM 알림을 일괄 발송합니다.

### C. UserRankingScheduler

* **Cron:** `0 0 0 * * SUN`
* **설명:** 매주 일요일 자정에 주간 리더보드를 초기화합니다. DB 주간 XP를 Bulk Update로 리셋하고 Redis `ranking:weekly:*` 키셋을 전체 삭제합니다.

---

## 🤝 5. FastAPI AI 서버 연동 엔드포인트 명세 (AiClient)

Spring Boot 백엔드가 FastAPI AI 서버를 호출하는 전체 엔드포인트 목록입니다.

| 메서드 | 엔드포인트 | 호출 시점 | 설명 |
|--------|-----------|-----------|------|
| POST | `/v1/news/analyze-batch` | Step 1 (newsStep) | 뉴스 임베딩/요약 생성 + 키워드 추출 + KeywordRelation 점수 반환 |
| POST | `/v1/news/build-news-relations` | Step 2 (relationStep) | 뉴스 간 벡터 유사도 기반 NewsRelation 생성 |
| POST | `/v1/recommend-keywords` | Step 3 (recommendStep) | 유저 임베딩 × 키워드 벡터 코사인 유사도 기반 개인화 추천 키워드 생성 |
| POST | `/v1/recommend-keywords/summarize` | Step 4 (recommendSummaryStep) / NeighborSummaryService | 추천 키워드 요약 본문 생성 |
| POST | `/v1/quizzes/generate` | QuizGenerationService (비동기) | 키워드 요약 기반 OX/객관식 퀴즈 생성 |
| POST | `/v1/users/init-embedding` | 온보딩 완료 후 비동기 (OnboardingAsyncService) | 유저 초기 임베딩 벡터 생성 |
| POST | `/v1/users/update-embedding` | 키워드 스크랩 추가 시 비동기 (UserVectorService) | 스크랩 키워드 반영하여 유저 임베딩 업데이트 |
| POST | `/v1/graph/preview` | 그래프 조회 API (GraphQueryService) | 키워드 노드/엣지 좌표 계산 및 그래프 레이아웃 반환 |

---

## 🔄 6. 데이터 파이프라인 흐름

```
[뉴스 API 수집]
      ↓
[Step 1: newsStep]
  ├─ DB 1차 저장 (news)
  ├─ FastAPI analyzeNewsBatch 호출
  │    ├─ 뉴스 임베딩/요약 생성
  │    ├─ 키워드 추출 (Persona/Macro/Specific 계층)
  │    └─ KeywordRelation 점수 행렬 반환
  ├─ 키워드 계층 구조화 (specificCache/macroCache로 N+1 방지)
  └─ KeywordRelation Upsert (findByPair → update/insert)
      ↓
[Step 2: relationStep]
  └─ 뉴스 간 벡터 유사도 기반 NewsRelation 적재
      ↓
[Step 3: recommendStep]
  └─ 유저별 관심사 임베딩 × 키워드 벡터 코사인 유사도 → RecommendKeyword 생성
      ↓
[Step 4: recommendSummaryStep] (병렬 - batchQuizExecutor)
  ├─ FastAPI summarizeKeywords 호출 → summary 저장
  └─ 추천 키워드 퀴즈 인라인 생성 (QuizGenerationService)
      ↓
[Step 5: neighborKeywordQuizStep] (병렬 - CompletableFuture)
  └─ 이웃 키워드(뉴스 연결된 소분류) 요약/퀴즈 병렬 생성
      ↓
[배치 완료]
  └─ Redis batch:graph 캐시 전체 삭제 → 다음 그래프 조회 시 최신 데이터 반영
```
---

## 📊 7. 모니터링 아키텍처 (Prometheus + Grafana)

### 모니터링 스택
- **Prometheus**: 메트릭 수집 (15초 간격 pull)
- **Grafana**: 대시보드 시각화 + 이메일 알림
- **Spring Boot Actuator + Micrometer**: 메트릭 노출 (`/actuator/prometheus`)
- **FastAPI prometheus-client**: Python 서버 메트릭 노출 (`/metrics`)

### 모니터링 대상

| 분류 | 메트릭 | 설명 |
|------|--------|------|
| JVM | `jvm_memory_used_bytes` | Heap/Non-Heap 메모리 사용량 |
| DB | `hikaricp_connections` | 커넥션 풀 상태 |
| 스케줄러 | `scheduler_job_success_total` | 배치 성공 횟수 |
| 스케줄러 | `scheduler_job_failure_total` | 배치 실패 횟수 |
| 스케줄러 | `scheduler_job_duration` | 배치 실행 시간 |
| AI | `ai_call_total` | AI API 호출 횟수 |
| AI | `ai_call_failure_total{error="RateLimitError"}` | OpenAI 429 에러 감지 |
| FastAPI | `fastapi_request_count` | API 요청 수 |
| FastAPI | `fastapi_request_latency_seconds` | API 응답시간 |

### 알림 규칙
- AI RateLimit 429 에러 발생 시 즉시 이메일
- 배치 Job 실패 시 즉시 이메일
- JVM Heap 메모리 90% 초과 시 이메일

### 접속 주소 (배포 환경)
- Prometheus: `https://nodingo-core.ddns.net/prometheus`
- Grafana: `https://nodingo-core.ddns.net/grafana`