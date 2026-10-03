# `/api/v2` 패키지 규칙

v1(`/api/v1/**`)은 운영 중이므로 동작을 바꾸지 않는다. v2는 같은 서버에서 이 패키지 아래에 병행한다.

## 구조

```
band.gosrock.api.v2
├── common/              # v2 공통 (페이징, 에러 정책)
└── <domain>/
    ├── controller/      # @RequestMapping("/api/v2/...")
    ├── usecase/         # 유스케이스 (도메인 서비스/어댑터 조합)
    └── dto/             # request / response
```

## 네이밍

- v2 클래스는 모두 `V2` 접두사를 붙인다 (`V2EventController`, `V2GetEventUseCase`, `V2EventResponse`).
  - Spring 기본 빈 이름은 단순 클래스명 기반이라, v1과 같은 이름이면 패키지가 달라도 빈 이름 충돌로 기동 실패한다.
- v2 컨트롤러 경로는 반드시 `/api/v2/` 로 시작하고 (Swagger `v2` 그룹, 공개 경로가 경로 기준), 클래스는 이 패키지 아래에 둔다 (403 정책이 핸들러 패키지 기준).

## 공통 동작

- 성공 응답: 기존 `SuccessResponseAdvice` 래핑을 그대로 사용 (`{success,status,data,timeStamp}`).
- 에러 응답: 기존 `ErrorResponse` 포맷 그대로. 단, 호스트 권한 실패(`V2ErrorPolicy.HOST_FORBIDDEN_CODES`)는 v2 핸들러(이 패키지의 컨트롤러)에서만 HTTP 403.
  - 403 정책은 MVC 핸들러 예외(`GlobalExceptionHandler`)에만 적용된다. 필터 단계에서 나는 예외는 대상이 아니다.
- 페이징: `V2PageResponse` 하나로 통일. `Slice` 기반이면 `totalElements`/`totalPages`가 `null`.
- 공개(비인증) 경로: `SecurityConfig.V2_PUBLIC_GET_PATHS` 에 추가.
- SUPER_ADMIN: `@HostRolesAllowed` 권한 검사만 건너뛴다. 요청자 역할을 보는 도메인 규칙은 그대로라, 멤버가 아니면 마스터 전용 규칙(매니저 추가·삭제, 마스터 양도)은 막히고 GUEST 추가·삭제, 역할 변경, 조회·수정은 된다.

## v1 / v2 로직 경계 (DEC-018)

- API 계층(컨트롤러·UseCase·DTO)은 이 패키지에 완전히 따로 둔다. v2 는 v1 api 클래스(UseCase·컨트롤러·DTO·mapper 등)를 쓰지 않는다. `api.common`, `api.config` 만 공유.
- 엔티티(테이블)는 v1 과 공유하고, 엔티티에는 **v1/v2 공통 불변식**만 둔다.
  - 상태 전이, 마스터 1명, 단순 getter
  - v1 ↔ v2 데이터 동기화: `Host.syncContactsFromV1` / `Host.replaceContacts`(첫 EMAIL·PHONE → v1 컬럼), `end_at` 기록(`Event.changeSchedule`), 첫 섹션 ↔ v1 content(`Event.replaceSections` / `syncIntroSectionFromV1`)
  - v2 전용 데이터의 최소 mutator 는 `internal`(Domain 모듈 안에서만 호출)로 두고, 검증·조합은 아래 서비스에서 한다.
- **v2 에만 있는 규칙**은 Domain 모듈 `band.gosrock.domain.domains.<domain>.service.v2.V2<Domain>DomainService` 에 둔다.
  - `V2EventDomainService`: 생성, 수정 가능 상태, 기본 정보 수정(hasTicket 잠금, 시작 시각), 문의처·태그·섹션 교체 검증, 섹션 대체 표시, 체크리스트, 등록, 삭제
  - `V2HostDomainService`: 프로필 부분 수정, 연락처 교체 검증·대체 표시, 멤버 즉시 추가·역할 변경·삭제
  - `V2HostFollowDomainService`: 팔로우 / 언팔로우
- v1 코드(v1 api, Admin, Domain 의 엔티티·v1 서비스)는 `..service.v2..` 를 호출하지 않는다. v2 서비스는 공유 v1 도메인 서비스(`EventService` 등)를 호출할 수 있다.

### 아키텍처 테스트

| 규칙 | 위치 |
|---|---|
| `api.v2` 밖의 api 클래스는 `api.v2` 에 의존하지 않음 (예외: `GlobalExceptionHandler`, `SwaggerConfig`) | `DuDoong-Api/src/test/kotlin/band/gosrock/api/architecture/V2ApiArchitectureTest.kt` |
| `api.v2` 는 `api.common` / `api.config` 외 v1 api 에 의존하지 않음 | 〃 |
| `api.v2` 밖(v1 api, Admin)은 `domain..service.v2..` 에 의존하지 않음 | 〃 |
| `service.v2` 밖의 Domain 클래스는 `domain..service.v2..` 에 의존하지 않음 | `DuDoong-Domain/src/test/kotlin/band/gosrock/domain/architecture/V2DomainServiceArchitectureTest.kt` |

예외를 늘려야 하면 테스트에 클래스를 명시적으로 추가하고 이유를 주석으로 남긴다.
