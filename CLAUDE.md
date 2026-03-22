# DuDoong Backend - Claude 컨텍스트

## 🎯 현재 진행 중인 작업

**Java → Kotlin 전체 마이그레이션**

마스터 트래킹 이슈: https://github.com/Gosrock/DuDoong-Backend/issues/582

---

## 🏗️ 프로젝트 아키텍처

### 기술 스택
- **언어**: Kotlin 1.9.22 (Java → Kotlin 마이그레이션 완료)
- **프레임워크**: Spring Boot 3.2.0
- **런타임**: Java 21
- **빌드**: Gradle 8.5 Kotlin DSL
- **DB**: MySQL + Spring Data JPA + QueryDSL
- **캐시/락**: Redis + Redisson (분산락)
- **외부 API**: Toss Payments (OpenFeign), AWS S3/SES, NCP AlimTalk, Slack API
- **인증**: Spring Security + JWT + Kakao OAuth
- **문서화**: SpringDoc OpenAPI (Swagger)
- **배치**: Spring Batch
- **실시간**: Spring WebSocket (STOMP)
- **코드 품질**: SonarQube, Spotless (google-java-format), Jacoco

### 멀티모듈 구조 (의존성 순서)

```
DuDoong-Backend/
├── DuDoong-Common/          # 최하위: JWT, 어노테이션, 예외처리, DTO, 유틸
├── DuDoong-Infrastructure/  # Common 의존: Redis, S3, SES, Feign, Slack, NCP
├── DuDoong-Domain/          # Common+Infra 의존: JPA 엔티티, 도메인 서비스, AOP
├── DuDoong-Api/             # 모든 모듈 의존: REST API, Security, Swagger
├── DuDoong-Socket/          # 모든 모듈 의존: WebSocket STOMP 서버
└── DuDoong-Batch/           # 모든 모듈 의존: Spring Batch 정산/알림/엑셀
```

### 도메인 영역 (DuDoong-Domain/domains/)

| 도메인 | 설명 |
|--------|------|
| `user` | 사용자 (Kakao OAuth 로그인) |
| `host` | 이벤트 주최자 |
| `event` | 이벤트 |
| `ticket_item` | 티켓 종류 |
| `order` | 주문/결제 (Toss Payments) |
| `cart` | 장바구니 |
| `coupon` | 쿠폰 |
| `issuedTicket` | 발급 티켓 (QR 입장 검증) |
| `settlement` | 정산 |
| `comment` | 댓글 |

### API 컨트롤러 목록

- `AuthController` - 카카오 OAuth 인증
- `UserController` - 유저 관리
- `HostController` - 호스트 관리
- `EventController` - 이벤트 CRUD
- `TicketItemController` / `TicketOptionController` - 티켓 종류
- `OrderController` / `OrderAdminController` - 주문/결제
- `CartController` - 장바구니
- `CouponController` - 쿠폰
- `IssuedTicketController` / `AdminIssuedTicketController` - 발급 티켓
- `ImageController` - S3 Pre-signed URL 이미지 업로드
- `AdminStatisticController` - 통계
- `CommentController` - 댓글

### GitHub 저장소

- **Remote**: https://github.com/Gosrock/DuDoong-Backend.git
- **기본 브랜치**: `dev`

---

## 🚀 Kotlin 마이그레이션 진행 상황

### 마이그레이션 전략: Bottom-up 방식

```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4
(Gradle)  (Common)  (Infra)   (Domain)   (Api)
                                    ↘  Phase 5 (Socket)
                                    ↘  Phase 6 (Batch)
```

### 이슈 목록

| Phase | 이슈 | 내용 | 상태 |
|-------|------|------|------|
| Master | [#582](https://github.com/Gosrock/DuDoong-Backend/issues/582) | 전체 트래킹 | 🔵 진행예정 |
| Phase 0 | [#583](https://github.com/Gosrock/DuDoong-Backend/issues/583) | Gradle Kotlin DSL 전환 | ⬜ 대기 |
| Phase 1-1 | [#584](https://github.com/Gosrock/DuDoong-Backend/issues/584) | Common: 어노테이션 & 유틸 | ⬜ 대기 |
| Phase 1-2 | [#585](https://github.com/Gosrock/DuDoong-Backend/issues/585) | Common: 예외처리 & DTO | ⬜ 대기 |
| Phase 1-3 | [#586](https://github.com/Gosrock/DuDoong-Backend/issues/586) | Common: JWT & Properties | ⬜ 대기 |
| Phase 2-1 | [#587](https://github.com/Gosrock/DuDoong-Backend/issues/587) | Infra: Redis & Redisson | ⬜ 대기 |
| Phase 2-2 | [#588](https://github.com/Gosrock/DuDoong-Backend/issues/588) | Infra: AWS S3 & SES | ⬜ 대기 |
| Phase 2-3 | [#589](https://github.com/Gosrock/DuDoong-Backend/issues/589) | Infra: Toss Payments Feign | ⬜ 대기 |
| Phase 2-4 | [#590](https://github.com/Gosrock/DuDoong-Backend/issues/590) | Infra: Slack & AlimTalk | ⬜ 대기 |
| Phase 3-1 | [#591](https://github.com/Gosrock/DuDoong-Backend/issues/591) | Domain: AOP & 도메인 이벤트 | ⬜ 대기 |
| Phase 3-2 | [#592](https://github.com/Gosrock/DuDoong-Backend/issues/592) | Domain: User & Host | ⬜ 대기 |
| Phase 3-3 | [#593](https://github.com/Gosrock/DuDoong-Backend/issues/593) | Domain: Event & TicketItem | ⬜ 대기 |
| Phase 3-4 | [#594](https://github.com/Gosrock/DuDoong-Backend/issues/594) | Domain: Order & Cart | ⬜ 대기 |
| Phase 3-5 | [#595](https://github.com/Gosrock/DuDoong-Backend/issues/595) | Domain: IssuedTicket & Coupon | ⬜ 대기 |
| Phase 3-6 | [#596](https://github.com/Gosrock/DuDoong-Backend/issues/596) | Domain: Settlement & Comment | ⬜ 대기 |
| Phase 4-1 | [#597](https://github.com/Gosrock/DuDoong-Backend/issues/597) | Api: Spring Security & Auth | ⬜ 대기 |
| Phase 4-2 | [#598](https://github.com/Gosrock/DuDoong-Backend/issues/598) | Api: User, Host, Event | ⬜ 대기 |
| Phase 4-3 | [#599](https://github.com/Gosrock/DuDoong-Backend/issues/599) | Api: Order, Cart, Coupon | ⬜ 대기 |
| Phase 4-4 | [#600](https://github.com/Gosrock/DuDoong-Backend/issues/600) | Api: IssuedTicket, TicketItem | ⬜ 대기 |
| Phase 4-5 | [#601](https://github.com/Gosrock/DuDoong-Backend/issues/601) | Api: Image, Statistic, 공통 | ⬜ 대기 |
| Phase 5 | [#602](https://github.com/Gosrock/DuDoong-Backend/issues/602) | Socket: WebSocket 서버 | ⬜ 대기 |
| Phase 6-1 | [#603](https://github.com/Gosrock/DuDoong-Backend/issues/603) | Batch: 정산 Job | ⬜ 대기 |
| Phase 6-2 | [#604](https://github.com/Gosrock/DuDoong-Backend/issues/604) | Batch: 만료/엑셀/Slack 통계 | ⬜ 대기 |

---

## 🔐 인증 및 권한 체계

### 인증 방식
- **로그인**: 카카오 OAuth (`/api/v1/auth/oauth/kakao`) 또는 로컬 개발용 (`/api/v1/auth/oauth/local/login`, dev 전용)
- **토큰 전달**: `accessToken` 쿠키 (기본) 또는 `Authorization: Bearer` 헤더
- **토큰 갱신**: `POST /api/v1/auth/token/refresh`

### 역할 (AccountRole)
| 역할 | 설명 |
|------|------|
| `USER` | 일반 유저 |
| `ADMIN` | 어드민 (내부 관리자) |
| `SUPER_ADMIN` | 최고 관리자 |

- **MANAGER 역할은 삭제됨** (호스트 멤버십의 HostRole과 혼동 방지)
- Role hierarchy: `SUPER_ADMIN > ADMIN > USER`

### API 접근 제어 (SecurityConfig)
| 경로 | 접근 권한 |
|------|----------|
| `/api/v1/auth/oauth/**` | permitAll |
| `/api/v1/events/{id}` (GET) | permitAll |
| `/api/v1/events/search` (GET) | permitAll |
| `/internal-api/**` | **ADMIN, SUPER_ADMIN만** |
| 나머지 `/api/**` | USER 이상 (인증 필수) |

### 호스트 권한 (HostRole AOP)
- `@HostRolesAllowed` AOP로 호스트 멤버십 기반 권한 체크
- **userId를 메서드 파라미터로 명시적 전달** (SecurityContext 미사용)
- `SUPER_ADMIN`은 호스트 멤버가 아니어도 모든 이벤트/호스트 접근 가능 (바이패스)
- HostQualification: `MASTER` > `MANAGER` > `GUEST`

### Admin API (internal-api)
- SecurityConfig 레벨: ADMIN/SUPER_ADMIN role 체크
- UseCase 레벨: `AdminAuthValidator`로 이중 권한 체크
  - 읽기: `validateAdminOrAbove` (ADMIN+)
  - 쓰기: `validateAdminOrAbove` (ADMIN+)
  - 역할 변경: `validateSuperAdmin` (SUPER_ADMIN만)
- 어드민 전용 로그인 엔드포인트 없음 — 일반 로그인 쿠키 사용

### 어드민 토큰 관련 (레거시)
- `X-Admin-Token` 헤더, `aud:admin` JWT claim — **사용하지 않음**
- `AdminLoginUseCase`, `AdminLocalDevLoginUseCase` — **삭제됨**
- 어드민 접근은 쿠키 기반 + DB role 체크로 통일

---

## 🧪 로컬 개발 & E2E 테스트

### 서버 기동 (로컬 MySQL 사용)
```bash
# docker-compose 먼저 (MySQL + Redis)
docker compose up -d

# local 프로필로 기동 — 반드시 이 방식으로!
./gradlew :DuDoong-Api:bootRun --args='--spring.profiles.active=local,infrastructure,domain,domain-local,common,common-local'
```

**주의**: `local` 프로필 없이 기동하면 **H2 인메모리 DB**를 사용하게 되어 MySQL과 불일치 발생.
- `spring.profiles.group.local` = `infrastructure, domain-local, common-local`
- `domain-local` 프로필이 `jdbc:mysql://127.0.0.1:13306/dudoong` 설정

### E2E 테스트 (Python pytest)
```bash
cd e2e-tests
pytest -v                          # 전체 실행
pytest test_33* test_34* -v       # 권한 테스트만
```

### 디버깅 팁
- API 안 되면 **프로필 먼저 확인** (`profiles are active` 로그)
- DB 연결 확인: 로그에서 `jdbc:mysql` vs `jdbc:h2:mem` 확인
- 403 나오면: DB에서 `account_role` 확인 + `hasAnyRole` 매칭 확인
- 한번에 안 되면 **curl로 한 단계씩 확인** (로그인 → DB 확인 → role 변경 → API 호출)

---

## 🔑 Kotlin 마이그레이션 핵심 원칙

1. **Lombok 제거**: `@Data` → `data class`, `@Builder` → named params + `copy()`, `@RequiredArgsConstructor` → 주생성자
2. **Null Safety**: `@Nullable`/`@NonNull` → Kotlin `?` / non-null 타입
3. **KAPT**: QueryDSL Q클래스 생성을 APT → KAPT로 전환
4. **JPA 엔티티**: `open class` 필수 (`allOpen` plugin), 기본 생성자 자동생성 (`noArg` plugin)
5. **Jackson**: `jackson-module-kotlin` 등록 필수
6. **Bean Validation**: `@field:NotBlank` 접두사 필수 (Kotlin data class)
7. **Spring Security**: Kotlin lambda DSL 활용

## 📋 PR 규칙

- 브랜치명: `feature/kotlin-migration-phase-{N}` 또는 `feature/kotlin-migration-phase-{N}-{name}`
- PR 본문: `close #이슈번호` 포함
- 각 Phase 완료 후 빌드(`./gradlew :{Module}:build`) 확인 필수

## ⚠️ 주요 주의사항

- Java ↔ Kotlin 혼재 허용 (마이그레이션 기간 중)
- CI (GitHub Actions) 빌드 항상 유지
- `lombok.config` 파일 → 최종 완료 시 삭제
- SonarQube 설정 → 마지막에 Kotlin 소스로 업데이트
