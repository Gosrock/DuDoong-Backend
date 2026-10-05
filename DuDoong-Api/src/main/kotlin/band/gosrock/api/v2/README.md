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
  - 옵션 응답 열 (I-3 발급 티켓, R-6 주문 #730): 두 엑셀은 **같은 규칙**(`V2OperationMapper.excelOptionColumnsOf`)으로 만든다. 열 집합은 대상·필터가 달라 다를 수 있다
    - 답변에 나온 옵션 그룹만 그룹 id 순 (soft delete 된 옵션도 지난 답변이 있으면 열이 남는다). 헤더는 질문 이름, 이름이 없거나 다른 옵션 또는 **그 엑셀의 기본 열**(`V2ExcelHeaders`)과 겹치면 `이름(그룹 id)`, 그래도 겹치면 `(그룹 id)` 를 한 번 더 붙인다 (예: 옵션 '입금자명' 은 R-6 에서만 `입금자명(id)`)
    - R-6 셀: 한 주문 한 행. 라인 1개면 응답 그대로, 여러 라인이면 같은 응답끼리 수량을 더해 `응답 ×수량` 을 **줄바꿈**으로 잇는다(응답 안의 쉼표와 구분, 셀 자동 줄바꿈). 응답 안의 줄바꿈은 공백, 빈 응답은 제외, 수량 없으면 1, 응답이 없으면 빈 칸
    - 수식 방어는 헤더(옵션 질문 이름)에도 적용. 라인 답변은 `V2OrderQuery.findAllForExport` 가 쿼리 1개로 미리 적재
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
  - 주문 검색 기준 `V2OrderSearchType` (#726): NAME(기본)·PHONE(현재 회원 정보, v1 어드민과 같음) + DEPOSITOR_NAME(주문에 저장된 입금자명, v2 두둥티켓 주문만). v1 `AdminTableSearchType` 은 v1·v2 발급 티켓 목록이 같이 써서 바꾸지 않는다. 주문 엑셀(R-6)에 입금자명 열(연락처 다음, 수식 방어 대상)
  - `V2NotificationDomainService` (#714): 알림센터(테이블 `tbl_notification` 은 v2 전용). 저장(`notify*`)은 커밋된 데이터를 다시 읽어 수신자·문구를 정하고 조건이 안 맞으면 저장 안 함, 목록(Slice)·안읽음 수·읽음(본인 것만, 남의 id 무시, 멱등)
    - 저장 핸들러 `api.v2.notification.handler.V2NotificationEventHandler`: 기존 도메인 이벤트(`CreateOrderEvent`·`DoneOrderEvent`·`WithDrawOrderEvent`, v1 경로 포함)와 v2 전용 `V2HostMembersAddedEvent`(`V2HostDomainService.addActiveHostUsers` 에서만 발행)에 붙는다. `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`, 예외는 삼키고 로그만 (원 트랜잭션 영향 없음)
    - 저장 핸들러는 알림 전용 executor(`notificationExecutor`, core 2 / max 4 / queue 200, 가득 차면 버리고 warn — 호출 스레드 실행 안 함)에서 돈다. 기존 `@Async` 기본 풀은 그대로. 결제형 주문·환불 등 이벤트 필드로 알 수 있는 비대상은 `condition`(SpEL)으로 큐에 넣기 전에 거른다
    - 중복 방지 uk(type, dedup_key, user_id): 주문 = orderUuid, 멤버 추가 = `host_user:{id}`. 일괄 저장은 `NotificationBulkRepository`(multi-row INSERT, 이미 있는 키는 건너뜀, 경합으로 uk 에 걸리면 행 단위 재시도)
    - 사용자 취소·환불 요청(#718, REFUND — v1 사용자 환불 포함) → 호스트 활성 마스터·매니저: 환불 요청이 걸리면 `ORDER_REFUND_REQUESTED`, 무료 취소는 `ORDER_CANCELED_BY_USER`. 카드(PG) 결제 주문은 condition 으로 제외
    - 두 알림 모두 엔티티가 발행하는 공통 도메인 이벤트에 붙으므로 v1 호스트 API·운영 어드민(`/internal-api/v1/orders/{uuid}/cancel`, `/internal-api/v1/refunds/{uuid}/complete`, `/internal-api/v1/orders/{uuid}/refund-status`) 경로도 저장된다. 운영 어드민(DuDoong-Admin 모듈)은 Api 서버에 같이 올라가 같은 `EventPublisherAspect`·핸들러를 쓴다
    - 호스트의 승인 후 취소(#726, v1 취소 포함) → 주문자 `ORDER_CANCELED_BY_HOST`: `WithDrawOrderEvent` 중 CANCELED 전부(결제 방식 무관 — 무료 선착순·카드 결제 포함, 결정 2026-10-05)에 붙고, 거절/취소 구분은 서비스가 `V2OrderStatus` 로 판정(둘 중 하나만 저장). 사용자 본인 취소(REFUND)는 대상 아님
    - 환불 완료(#726, v1 호스트 환불 완료 포함) → 주문자 `ORDER_REFUND_COMPLETED`: `Order.completeRefund` 가 `RefundCompletedOrderEvent`(uuid·결제 방식)를 발행, 결제형(PAYMENT)은 condition 으로 거른다. 거절·취소·철회(CANCELED·REFUND) 상태 + 돌려준 돈이 있는 주문만(서비스가 판정 — v1·운영 어드민은 상태 검사 없이 환불 완료로 바꿀 수 있어 승인 완료 주문 등은 저장 안 함). 다시 완료해도 uk 로 1건
    - 알림 없음: v1 초대 → 수락 경로(본인이 수락하므로 "추가됨" 알림 대상 아님, `HostUserJoinEvent` 는 슬랙만)
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
  - P-5: 유효 + `TicketItem.isOnSale` 티켓만(지난 공연도 목록은 보임). `isPurchasable` = `V2TicketItemDomainService.isPurchasableInV2App`(v2 결제 방식 DUDOONG/FREE + 공연 OPEN + 시작 전 + 판매 중 + 재고 > 0, 기존 PG 티켓은 false). 티켓·옵션 그룹은 `V2TicketItemQuery` fetch join(옵션 N+1 없음) + 티켓별 승인 대기 수량 그룹 쿼리 1개(쿼리 4개). 계좌는 로그인한 결제 화면(O-0)에서만 제공(P-5 응답에 없음, ArchUnit 으로 고정). 잔여 = 재고 - 승인 대기 수량(#726, `V2TicketItemDomainService.availableQuantity` — 주문 재고 검사 #723 과 같은 기준), 재고 공개 + 수량 지정일 때만 값. 매진·구매 가능도 같은 잔여로 판정. 호스트 T-1(티켓 관리)·D-1(대시보드)은 기존 `remaining`·`soldCount` 의미(재고 기준)를 그대로 두고 `pendingApproveCount`(승인 대기 수량)를 따로 준다 — 같은 그룹 쿼리(결정 2026-10-05). T-1 `hasPendingOrders` 도 이 값 > 0
- `V2UserOrderDomainService` / `V2UserOrderQuery` / `V2MyOrderStatus` (#718): 사용자 앱 주문(O-1~O-4)
  - 생성: v1 장바구니 → 주문을 한 트랜잭션에서. **장바구니는 저장하지 않는다**(메모리 `Cart` 로 v1 `CartValidator` 검증만 — 주문 생성 때 v1 '최근 장바구니'를 덮어쓰지 않음), 주문은 v1 `OrderFactory.createNormalOrder(cart, userId)` 규칙 그대로. 단 주문이 **완료(승인·무료 확정)되면 v1 과 같이 그 사용자의 v1 장바구니가 지워진다**(`DoneOrderEvent` → `DoneOrderEventHandler`, v1 기존 동작)
  - 무료 선착순은 이어서 v1 `FreeOrderService`(별도 락·트랜잭션, 발급이 커밋된 주문을 읽기 때문). 확정이 실패하면 `failUnconfirmed` 로 주문을 FAILED 처리(새 트랜잭션)하고 원래 오류를 돌려준다 — 재시도는 새 주문
  - 락: `티켓관리:{ticketItemId}` (v1 발급·재고 감소, v2 티켓 수정과 같은 락). v1 주문 생성의 `주문생성:{userId}` 보다 넓다 — 승인 대기 재고·1인 제한 검사가 다른 사용자 동시 주문에도 맞고, 티켓 조건 변경과 주문이 겹치지 않는다
  - 1인 제한 실제 보장 범위: v2 주문끼리는 같은 티켓 락으로 줄 세워 보장(승인형 = v1 승인 대기 합산 검사, 무료 선착순 = 발급 수 + **확정 전 v2 주문 수량(최근 5분)** 합산). v1 앱과 v2 앱에서 같은 사용자가 동시에 주문하면 v1 쪽이 이 락을 잡지 않아 보장하지 않는다(v1 끼리도 원래 같은 틈)
  - MySQL 격리 수준 의존: v1 무료 확정은 발급(REQUIRES_NEW 커밋) 뒤 같은 트랜잭션에서 재고·1인 제한을 다시 세는데, MySQL(REPEATABLE READ)은 트랜잭션 첫 조회 스냅샷이라 방금 발급분을 세지 않고 H2(READ COMMITTED)는 센다. 그래서 '마지막 1장'·제한 경계는 MySQL E2E(test_45)에서 검증하고 H2 통합 테스트는 경계를 피한다
  - 옵션: 일괄 = 라인 1개(수량 N), 티켓별 = **수량 1 라인 N개** (v1 장바구니가 원래 지원하는 구조, 발급 시 라인 답변이 티켓 답변으로 복사). 네/아니오 답은 `YES`/`NO`(예·네 / 아니요·아니오 허용), 저장값은 v1 과 같은 `예`/`아니요`
  - 결제 방식: 두둥 = BANK_TRANSFER / TOSS_TRANSFER + 입금자명 1~20자, 무료 = FREE. `tbl_order.payment_channel`·`depositor_name`(V007, v1 주문은 null). PG 티켓은 `Order_400_20`
  - 중복 요청: 같은 사용자·티켓·라인 수량·옵션 답변·결제 방식·입금자명이 10초 안에 다시 오면 앞 주문(진행 중·완료)을 돌려준다(스칼라 조회). 무료 선착순이 아직 확정 전이면 `Order_400_26` (이중 확정 방지). `Idempotency-Key` 헤더는 지원하지 않는다
  - 상태(`V2MyOrderStatus`): 호스트 분류와 같고 사용자 철회(REFUND)를 REFUNDED(환불 요청/완료) / CANCELED(환불 NONE = 무료 취소)로 나눈다. 목록 제외 READY·PENDING_PAYMENT·FAILED·OUTDATED (v1 마이페이지 목록과 같음)
  - 취소: `주문:{uuid}` 락. 승인 대기 = 공연 OPEN + 시작 전, 승인 완료 = + 입장·선물 대기·선물 완료(주문자 소유 아님) 티켓 없음 (#719). 카드(PG) 결제 주문은 `Order_400_24`. 승인 완료 유료는 v1 `Order.refund`, 그 외(승인 대기, 무료 승인)는 `Order.withdrawByUser`(internal) — 둘 다 상태 REFUND(v1 메일·슬랙이 '구매자 환불'로 처리), 유료만 환불 요청 + 환불 계좌(`tbl_order_refund_account`, 필수)
  - 환불 계좌 노출: 호스트 R-2 상세·F-1 환불 목록·변경 응답에서 **매니저 이상(+SUPER_ADMIN)만** 전체, 일반 멤버는 null. 주문자 O-3 은 계좌번호 뒤 4자리
  - 발급 티켓(O-3)은 주문자 소유분 + 내가 선물해 수락된 티켓(`giftState=SENT`, uuid 없음, #719). 취소 차단에 선물 대기·선물 완료 티켓 포함
  - 결제 화면 O-0 `GET /api/v2/events/{eventId}/ticket-items/{ticketItemId}/checkout` (#726, 로그인): P-5 와 같은 티켓 + 입금 계좌(두둥티켓 + `isPurchasable` 일 때만 — 지난 공연·정산중·종료·매진은 null, O-3 `payment.account` 와 같은 형태). 티켓 응답은 P-5 와 같은 `V2PublicTicketItemMapper` 로 만든다. [결제하기]·토스 송금 전에 계좌를 보여 주기 위한 것이라 공개 경로에 넣지 않는다. 판매 중 아닌 티켓·다른 공연 티켓은 404
  - 승인형 1인 제한 = 발급 수 + 같은 사용자·같은 티켓 승인 대기 수량 + 이번 수량 (v1 `OrderValidator.validApproveStatePurchaseLimit`, `Order.createApproveOrder` 에서 v1·v2 공통). v2 는 `티켓관리` 락 안이라 같은 사용자 동시 주문도 보장(#726 동시성 테스트), v1 은 `주문생성:{userId}` 락으로 v1 끼리 보장
- `V2TicketGiftDomainService` / `V2MyTicketQuery` / `V2TicketGiftCascadeHandler` (#719, `domains.gift.service.v2`): 사용자 앱 티켓탭(T-1·T-2)·선물(G-1~G-8). 테이블 `tbl_ticket_gift` 는 v2 전용(V008)
  - 잠금 순서 **주문 → 티켓** 하나: 선물 전이(생성·회수·수락·거절·반환·메모)는 `주문:{orderUuid}` 락(v1·v2 승인·거절·취소·환불과 같은 락, 새 트랜잭션) 안에서 티켓 행 `SELECT ... FOR UPDATE` → 선물 행 잠금 읽기 후 판정. 락 전에는 orderUuid 만 스칼라로 읽는다
  - 연쇄 처리는 도메인 이벤트 **BEFORE_COMMIT**(원 트랜잭션 안, 실패하면 원 전이도 롤백): `WithDrawOrderEvent`(v1·v2 호스트 취소, 운영 취소 — 대기 선물 CANCELED(ORDER_CANCELED), 선물 완료 티켓은 v1 티켓 철회 핸들러가 함께 취소), `UserDeactivatedEvent`(탈퇴·운영 정지 → SENDER_WITHDRAWN), `EventAdminStatusChangeEvent`(운영 삭제·준비중 전환 → EVENT_REMOVED, 정산중·지난공연은 대기 유지). 주문 연쇄는 대기 선물을 먼저 찾아(`idx_ticket_gift_order_uuid_status`) 없으면 끝내고(선물 없는 주문은 인덱스 조회 1회), 있으면 그 티켓 행만 PK 로 잠근다 — v1 티켓 철회 핸들러보다 먼저(`@Order`). 선물 완료 티켓은 철회 핸들러가 취소하고 기록은 ACCEPTED 그대로라 잠그지 않는다(반환 G-6 은 같은 주문 락)
  - 운영 어드민 주문 취소(`AdminCancelOrderUseCase`)는 v1 호스트 취소와 같은 `WithdrawOrderService.cancelOrder`(주문 락)로 바꿨다 (#719 — 선물 수락과 같은 락으로 줄 서도록)
  - 수락·반환 때 `IssuedTicket.transferOwner`(internal)로 소유자 정보 + uuid(QR) 교체 (`V2TicketUuidIssuer`, 이미 있는 uuid 면 다시 뽑음). 옛 uuid 는 v1·v2 모두 없는 티켓
  - 선물 만료(공연 종료) = `V2EventDisplayRule` PAST (종료 시각 경과 또는 CALCULATING·CLOSED·DELETED). 상태는 PENDING 유지, 배치 없음
  - 토큰: 32바이트 SecureRandom base64url. 요청 로그·Slack URL 에서 `SensitiveBodyMasker.maskPath` 로 가린다. 메모(`memo`)는 본문 마스킹 키
  - 알림: `V2GiftNotificationDomainService` + `api.v2.notification.handler.V2GiftNotificationEventHandler`(AFTER_COMMIT, 알림 전용 풀). 보낸 사람 GIFT_SENT·ACCEPTED·REJECTED·RETURNED, 받은 사람 GIFT_RECEIVED·GIFT_TICKET_CANCELED. 회수는 알림 없음. dedup = `gift:{giftId}`. 호스트·운영 취소 때 주문자(보낸 사람)는 #726 의 ORDER_CANCELED_BY_HOST, 받은 사람은 GIFT_TICKET_CANCELED 만 받는다 (수신자가 달라 겹치지 않음, 운영 경로 포함)
- **v1 공통 보호** `domains.gift.service.TicketGiftGuard` (#719, `service.v2` 밖 — v1 코드가 부른다, 읽기만): v1 입장(티켓 행 잠금 뒤 선물 행 **잠금 읽기**(`FOR SHARE`) — 일반 읽기는 REPEATABLE READ 스냅샷이라 잠금 대기 중 커밋된 선물 생성을 놓친다. 선물 대기면 `IssuedTicket_400_8`), v1 티켓 상세(대기면 400_8), v1 주문 티켓 목록(주문자 소유분만, 대기는 uuid null), v1 사용자 환불(`OrderValidator.validCanRefund`, 선물 대기·완료면 `Order_400_24`), v2 체크인(같은 잠금 읽기로 GIFT_PENDING, 잠근 뒤 uuid·소유자가 요청과 다르면 OTHER_EVENT). 1인 제한 `countPaidTicket` 은 원 구매자(주문 사용자) 기준 (선물 없으면 기존과 같은 값)
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
| 엔티티의 v2 `internal` mutator(`changeHasTicket`, `changeSchedule`, `changePosterImage`, `changePlace`, `replaceContacts`, `replaceTagIds`, `replaceSections`, `getOrInitProfile`, `TicketItem.changeAccountInfo`, `TicketItem.changeSupplyCount`, `Order.recordRefuseReasonType`, `Order.recordV2Payment`, `Order.withdrawByUser`, `IssuedTicket.transferOwner`, `TicketGift.accept/reject/returnToSender/cancel/changeMemo`)는 `service.v2` 와 엔티티 자신(`Event`/`Host`/`TicketItem`/`Order`/`IssuedTicket`/`TicketGift`)만 호출 | `DuDoong-Domain/src/test/kotlin/band/gosrock/domain/architecture/V2DomainServiceArchitectureTest.kt` |
| `V2*DomainService` 는 `..service.v2..` 패키지에 둔다 | 〃 |
| 선물 저장소 `TicketGiftRepository`(v2 전용 테이블)는 `service.v2` 와 v1 보호용 `TicketGiftGuard` 만 접근 (#719) | 〃 |
| 공개 공연 탐색 컨트롤러·유스케이스·응답은 계좌(`AccountInfoVo`, `V2TicketAccountResponse`)에 의존하지 않음 | `V2ApiArchitectureTest` |
| 공개 공연 탐색 응답 DTO 는 `@Entity` 클래스에 의존하지 않음 | 〃 |
| 엔티티의 **모든** Kotlin `internal` 메서드(`*$DuDoong_Domain`, 자동 수집)는 `service.v2` 와 그 엔티티 자신만 호출, 수집 결과 = 위 목록 (#721) | `V2DomainServiceArchitectureTest` |
| 공개 GET 추가로 호스트 전용 경로·다른 메서드가 열리지 않음 (비로그인 401). `RequestMappingHandlerMapping` 의 `/api/v2/` 매핑을 자동 열거해 공개 GET 밖 전부 검사 (#721) | `DuDoong-Api/src/test/kotlin/band/gosrock/api/v2/V2AuthRequiredPathsTest.kt` |
| DEC-013 호스트 삭제 미제공: `DELETE /api/v2/hosts/{id}` 매핑 없음, 호출 시 405 | `V2HostControllerTest.NoHostDelete` |

- 예외를 늘려야 하면 테스트에 클래스를 명시적으로 추가하고 이유를 주석으로 남긴다.
- 새 v2 internal mutator 를 엔티티에 추가하면 Domain 테스트의 `V2_INTERNAL_MUTATORS_BY_OWNER`(엔티티별) 에도 추가한다 (Kotlin `internal` 은 JVM 이름이 `name$모듈명` 으로 맹글링되어 접두로 매칭). 빠뜨리면 자동 수집 대조 테스트가 실패한다.
- 공개(비인증) v2 경로를 추가하면 `V2AuthRequiredPathsTest.PUBLIC_GET` 에도 추가한다 (기대값을 SecurityConfig 에서 읽지 않는다). 새 인증 필요 경로는 목록 수정 없이 자동 검사된다.
- **한계**: ArchUnit 은 바이트코드 의존만 본다. `const val` 상수(예: `V2EventDomainService.MAX_CONTACT_COUNT` 를 v2 DTO 어노테이션에서 사용)와 `inline` 함수는 호출 측에 값·본문이 인라인되어 의존이 남지 않으므로 검사되지 않는다. 같은 모듈 안의 `internal` 호출 제한은 `@Entity` 클래스의 internal 메서드만 대상이다 (엔티티 밖 internal 은 검사 안 함).
