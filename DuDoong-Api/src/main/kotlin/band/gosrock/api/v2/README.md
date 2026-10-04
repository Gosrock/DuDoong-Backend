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
- 페이징: `V2PageResponse` 하나로 통일 (`Slice` 기반이면 `totalElements`/`totalPages`가 `null`). 건수가 함께 필요한 목록(주문·발급 티켓)은 `{counts, orders|tickets: V2PageResponse}`.
- 엑셀: 운영 어드민 `AdminExcelService.generateTableExcel(escapeFormula = true)` 재사용, `ResponseEntity<ByteArray>` 로 내려 성공 응답 래핑을 타지 않는다.
  - 수식 인젝션 방어: `= + - @ 탭 CR` 로 시작하는 문자열 셀 앞에 `'` (v2 엑셀만, 기존 운영 어드민 엑셀은 그대로)
  - 행 상한 10,000 (`v2.export.max-rows`), 넘으면 400 (`Order_400_19` / `IssuedTicket_400_7`). 연관 데이터는 fetch join
  - 개인정보: 연락처는 포함(입금 확인용, v1 수준), 이메일은 제외(주문 상세에서만). 다운로드마다 감사 로그(userId, eventId, 필터, 행 수)
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
  - `V2OrderDomainService` (#712): 주문의 공연 소속 확인(다른 공연 주문은 404), 승인·취소(v1 `OrderApproveService`·`WithdrawOrderService` 그대로 호출), 거절(사유 종류 검증 + `Order.recordRefuseReasonType`, 표시 문구는 v1 `cancel_reason`), 환불 완료(요청 상태만, 완료는 멱등). 상태 분류는 `V2OrderStatus` (v1 상태값 유지, REFUSED = CANCELED + 사유 종류 있음 또는 approved_at 없음)
  - `V2CheckInDomainService` (#712): 체크인 결과 판정(ENTERED / ALREADY_ENTERED / OTHER_EVENT / CANCELED, 셀프 전용 SELECT_TICKET), 입장은 v1 `IssuedTicket.entrance` + 행 잠금(`SELECT ... FOR UPDATE`)으로 동시 스캔 1회만 입장. 호스트 스캔은 OPEN·CALCULATING(지각 입장) 공연만(그 외 `Event_400_27`), 셀프 체크인은 OPEN 만
  - 셀프 체크인 토큰(Q-4)은 공연 등록 때가 아니라 **최초 조회 시 생성**한다(조건부 UPDATE, 동시 조회도 토큰 1개). `Event.checkInToken` 은 읽기 전용 매핑이라 생성 직후 엔티티 값은 stale 일 수 있다
  - 조회 쿼리 `V2OrderQuery` / `V2IssuedTicketQuery` 도 `service.v2` 에 둔다 (v2 만 사용)
  - `V2NotificationDomainService` (#714): 알림센터(테이블 `tbl_notification` 은 v2 전용). 저장(`notify*`)은 커밋된 데이터를 다시 읽어 수신자·문구를 정하고 조건이 안 맞으면 저장 안 함, 목록(Slice)·안읽음 수·읽음(본인 것만, 남의 id 무시, 멱등)
    - 저장 핸들러 `api.v2.notification.handler.V2NotificationEventHandler`: 기존 도메인 이벤트(`CreateOrderEvent`·`DoneOrderEvent`·`WithDrawOrderEvent`, v1 경로 포함)와 v2 전용 `V2HostMembersAddedEvent`(`V2HostDomainService.addActiveHostUsers` 에서만 발행)에 붙는다. `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`, 예외는 삼키고 로그만 (원 트랜잭션 영향 없음)
    - 저장 핸들러는 알림 전용 executor(`notificationExecutor`, core 2 / max 4 / queue 200, 가득 차면 버리고 warn — 호출 스레드 실행 안 함)에서 돈다. 기존 `@Async` 기본 풀은 그대로. 결제형 주문·환불 등 이벤트 필드로 알 수 있는 비대상은 `condition`(SpEL)으로 큐에 넣기 전에 거른다
    - 중복 방지 uk(type, dedup_key, user_id): 주문 = orderUuid, 멤버 추가 = `host_user:{id}`. 일괄 저장은 `NotificationBulkRepository`(multi-row INSERT, 이미 있는 키는 건너뜀, 경합으로 uk 에 걸리면 행 단위 재시도)
    - 알림 없음: v1 초대 → 수락 경로(본인이 수락하므로 "추가됨" 알림 대상 아님, `HostUserJoinEvent` 는 슬랙만), 승인 후 취소·사용자 환불
    - 처리가 끝난 승인 대기 알림(승인·거절된 주문)을 자동으로 읽음 처리하지 않는다 (후속)
    - 거절 사유는 본문에 100자 + `…`, extra 값은 각각 300자 + `…` 로 자른 뒤 직렬화
- `V2EventBrowseDomainService` / `V2EventBrowseQuery` (#716): 사용자 앱 공연 탐색(P-1~P-5, 모두 비로그인)
  - 공개 공연 = OPEN·CALCULATING·CLOSED. 준비중·삭제는 상세(P-3)·티켓(P-5)에서 멤버여도 404, 홈·리스트에 절대 미포함 (호스트는 E-3 관리 화면)
  - 표시 상태는 `V2EventDisplayRule` 한 곳에서 판정(호스팅 센터 E-1·E-3·E-8·E-9·H-14 와 공통, enum `V2EventDisplayStatus`): PREPARING / OPEN 시작 전 UPCOMING / OPEN 시작~종료 전 ONGOING / 그 외 PAST. 종료 = startAt + runTime(DEC-019 #6), runTime 없으면 종료 = 시작, startAt 없으면 PAST (방어 — prod 0건)
  - 홈(P-1)·P-2 includePast=false = 종료 전 등록 공연(ONGOING + UPCOMING). 쿼리 조건은 종료 배치와 같은 `TIMESTAMPADD(MINUTE, run_time, start_at) > now`, start_at 하한 보조 조건 없음(runTime 상한 없음, OPEN 행 적음 — V006 주석)
  - P-2 정렬 UPCOMING: 종료 전 그룹 시작 임박순(진행 중이 앞) → PAST 그룹 최근 시작 순, 같은 시작이면 id 순. includePast=true 면 종료된 OPEN·CALCULATING·CLOSED 를 PAST 로 포함
  - 검색어 = 공연명 OR 호스트명 부분일치(최대 50자, 대소문자 무시, `%`·`_`·`!` 는 QueryDSL contains 가 이스케이프). 태그 = 같은 분류 OR / 분류끼리 AND(분류별 EXISTS), 없는 태그 id 는 400 `Event_400_23`, 51개 이상은 요청 검증 400
  - 목록의 태그·호스트명은 페이지 단위 일괄 조회(호스트는 스칼라 조회 — `Host.hostUsers` EAGER 회피)
  - 문의처: 공연 문의처, 없으면 `V2HostDomainService.displayContacts`(v2 연락처 → v1 전화/이메일)
  - P-5: 유효 + `TicketItem.isOnSale` 티켓만(지난 공연도 목록은 보임). `isPurchasable` = `V2TicketItemDomainService.isPurchasableInV2App`(v2 결제 방식 DUDOONG/FREE + 공연 OPEN + 시작 전 + 판매 중 + 재고 > 0, 기존 PG 티켓은 false). 티켓·옵션 그룹은 `V2TicketItemQuery` fetch join(옵션 N+1 없음, 쿼리 3개) 계좌는 주문 단계에서 제공(응답에 없음, ArchUnit 으로 고정). 잔여 = 재고 공개 + 수량 지정일 때만
- **open-in-view**: test·staging·prod 는 켜져 있다(기본값). 요청 영속성 컨텍스트에 먼저 올린 엔티티는 락 트랜잭션(REQUIRES_NEW)에서 바뀌어도 같은 요청 안에서 갱신되지 않으므로, 락 서비스를 부르기 전 검사는 엔티티 대신 스칼라 조회로 한다 (`V2OrderDomainService.validateEventOrder`)
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
| 엔티티의 v2 `internal` mutator(`changeHasTicket`, `changeSchedule`, `changePosterImage`, `changePlace`, `replaceContacts`, `replaceTagIds`, `replaceSections`, `getOrInitProfile`, `TicketItem.changeAccountInfo`, `TicketItem.changeSupplyCount`, `Order.recordRefuseReasonType`)는 `service.v2` 와 엔티티 자신(`Event`/`Host`/`TicketItem`/`Order`)만 호출 | `DuDoong-Domain/src/test/kotlin/band/gosrock/domain/architecture/V2DomainServiceArchitectureTest.kt` |
| `V2*DomainService` 는 `..service.v2..` 패키지에 둔다 | 〃 |
| 공개 공연 탐색 컨트롤러·유스케이스·응답은 계좌(`AccountInfoVo`, `V2TicketAccountResponse`)에 의존하지 않음 | `V2ApiArchitectureTest` |
| 공개 공연 탐색 응답 DTO 는 `@Entity` 클래스에 의존하지 않음 | 〃 |
| 공개 GET 추가로 호스트 전용 경로·다른 메서드가 열리지 않음 (비로그인 401) | `DuDoong-Api/src/test/kotlin/band/gosrock/api/v2/V2AuthRequiredPathsTest.kt` |

- 예외를 늘려야 하면 테스트에 클래스를 명시적으로 추가하고 이유를 주석으로 남긴다.
- 새 v2 internal mutator 를 엔티티에 추가하면 Domain 테스트의 `V2_INTERNAL_MUTATORS` 목록에도 추가한다 (Kotlin `internal` 은 JVM 이름이 `name$모듈명` 으로 맹글링되어 접두로 매칭).
- **한계**: ArchUnit 은 바이트코드 의존만 본다. `const val` 상수(예: `V2EventDomainService.MAX_CONTACT_COUNT` 를 v2 DTO 어노테이션에서 사용)와 `inline` 함수는 호출 측에 값·본문이 인라인되어 의존이 남지 않으므로 검사되지 않는다. 같은 모듈 안의 `internal` 호출 제한도 위 목록 기반 규칙으로만 잡힌다.
