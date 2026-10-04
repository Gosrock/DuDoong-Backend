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
  - `V2TicketItemDomainService`: 티켓 생성(티켓 없음 공연 거부, DUDOONG/FREE), 폼 전체 수정(잠기면 DEC-006 허용 필드만, 잠긴 필드는 양쪽 trim 비교·바뀐 값만 길이 검증), 판매 기간 검증, 판매 상태(`saleState`)·구매 가능 판정, **잠금 판정(`isLocked` = 재고 감소 OR 승인 대기 주문)**, 판매 중단·재개(멱등), 옵션 전체 지정
  - `V2TicketOptionDomainService`: 옵션 생성(SUBJECTIVE / YES_NO=TRUE_FALSE), 부분 수정(잠긴 티켓에 붙으면 이름·설명만, DEC-012), 삭제(잠긴 티켓에 붙으면 불가, 판매 전 티켓에서는 떼고 삭제). 수정·삭제는 붙은 티켓들의 `티켓관리:{id}` 락을 id 순으로 잡고 판정
- 티켓 공통 불변식(엔티티 `TicketItem`): 재고 감소 = 판매됨(`isSold`), 판매된 티켓 옵션 변경·삭제 불가, 무제한·매수 제한 없음 저장값(`TicketItem.UNLIMITED_SUPPLY_COUNT` / `NO_PURCHASE_LIMIT` = 1,000,000, `isUnlimitedSupply()` / `hasNoPurchaseLimit()` — v1 응답·어드민·v2 공통), **판매 중 판정(`isOnSale`: isSellable + 판매 기간)**
  - v1 장바구니·주문 생성(`CartValidator`/`OrderValidator.validCanCreate`)이 이 검사를 하고, v1 공개 티켓 목록은 판매 중인 티켓만 보여 준다(어드민 목록은 전부). v1 로 만든 티켓은 isSellable=true·기간 null 이라 영향 없음
- v1 코드(v1 api, Admin, Domain 의 엔티티·v1 서비스)는 `..service.v2..` 를 호출하지 않는다. v2 서비스는 공유 v1 도메인 서비스(`EventService` 등)를 호출할 수 있다.

### 아키텍처 테스트

| 규칙 | 위치 |
|---|---|
| `api.v2` 밖의 api 클래스는 `api.v2` 에 의존하지 않음 | `DuDoong-Api/src/test/kotlin/band/gosrock/api/architecture/V2ApiArchitectureTest.kt` |
| 예외: `GlobalExceptionHandler` / `SwaggerConfig` 는 `api.v2` 중 `V2ErrorPolicy` 에만 의존 가능 | 〃 |
| `api.v2` 는 `api.common` / `api.config` 외 v1 api 에 의존하지 않음 | 〃 |
| `domain..service.v2..` 에는 `api.v2..` 와 `domain..service.v2..` 만 의존 가능 (허용 목록. v1 api, Admin, Domain, Infrastructure, Common 전부 금지) | 〃 (Api classpath), `DuDoong-Domain/.../architecture/V2DomainServiceArchitectureTest.kt`, `DuDoong-Batch/src/test/kotlin/band/gosrock/architecture/V2BatchArchitectureTest.kt` |
| 엔티티의 v2 `internal` mutator(`changeHasTicket`, `changeSchedule`, `changePosterImage`, `changePlace`, `replaceContacts`, `replaceTagIds`, `replaceSections`, `getOrInitProfile`, `TicketItem.changeAccountInfo`, `TicketItem.changeSupplyCount`)는 `service.v2` 와 엔티티 자신(`Event`/`Host`/`TicketItem`)만 호출 | `DuDoong-Domain/src/test/kotlin/band/gosrock/domain/architecture/V2DomainServiceArchitectureTest.kt` |
| `V2*DomainService` 는 `..service.v2..` 패키지에 둔다 | 〃 |

- 예외를 늘려야 하면 테스트에 클래스를 명시적으로 추가하고 이유를 주석으로 남긴다.
- 새 v2 internal mutator 를 엔티티에 추가하면 Domain 테스트의 `V2_INTERNAL_MUTATORS` 목록에도 추가한다 (Kotlin `internal` 은 JVM 이름이 `name$모듈명` 으로 맹글링되어 접두로 매칭).
- **한계**: ArchUnit 은 바이트코드 의존만 본다. `const val` 상수(예: `V2EventDomainService.MAX_CONTACT_COUNT` 를 v2 DTO 어노테이션에서 사용)와 `inline` 함수는 호출 측에 값·본문이 인라인되어 의존이 남지 않으므로 검사되지 않는다. 같은 모듈 안의 `internal` 호출 제한도 위 목록 기반 규칙으로만 잡힌다.
