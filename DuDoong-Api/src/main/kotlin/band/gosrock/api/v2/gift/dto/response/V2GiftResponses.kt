package band.gosrock.api.v2.gift.dto.response

import band.gosrock.api.v2.gift.dto.V2MyTicketState
import band.gosrock.api.v2.operation.dto.response.V2OptionAnswerResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderEventResponse
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.gift.domain.TicketGiftCancelReason
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.gift.service.v2.V2GiftState
import band.gosrock.domain.domains.gift.service.v2.V2GiftViewState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.service.v2.V2MyOrderStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// ===== 티켓탭 T-1 / T-2 =====

/** T-1 내 티켓 (주문 단위 묶음, 공연 임박순) */
data class V2MyTicketsResponse(
    val groups: List<V2MyTicketGroupResponse>,
)

data class V2MyTicketGroupResponse(
    @field:Schema(description = "묶음 키 (목록 key 용). 내 주문 = 주문 uuid, 받은 티켓 묶음 = ticket:{첫 티켓 번호}")
    val groupKey: String,
    @field:Schema(description = "주문 uuid. 내 주문일 때만 (받은 티켓 묶음은 null — T-2 와 같은 기준)")
    val orderUuid: String?,
    @field:Schema(description = "예매 번호. 내 주문일 때만 (받은 티켓 묶음은 null)")
    val orderNo: String?,
    @field:Schema(description = "주문자 = 나. false(받은 티켓)면 [주문상세] 없음")
    val isMyOrder: Boolean,
    @field:Schema(description = "주문 상태 (내 주문일 때만)")
    val orderStatus: V2MyOrderStatus?,
    @field:Schema(description = "거절 사유 종류·문구 (REFUSED 일 때)")
    val refuseReasonType: OrderRefuseReasonType?,
    val refuseReason: String?,
    val event: V2MyOrderEventResponse?,
    val ticketName: String?,
    @field:Schema(description = "화면 상태별 개수 (예: {APPROVED: 2, GIFT_PENDING: 1}). 티켓이 없는 승인 대기·거절 주문은 주문 수량")
    val counts: Map<V2MyTicketState, Int>,
    @field:Schema(description = "티켓 (발급 순). 승인 대기·거절 주문은 빈 목록")
    val tickets: List<V2MyTicketElement>,
)

data class V2MyTicketElement(
    @field:Schema(description = "티켓 uuid (T-2·G-1·G-6 경로 값). 선물 완료(SENT) 행은 null — 받은 사람의 QR 이다")
    val ticketUuid: String?,
    @field:Schema(description = "티켓 번호 (T1000xxxx)")
    val issuedTicketNo: String?,
    val ticketName: String?,
    @field:Schema(description = "유료 옵션 추가금 합(원). #755: optionPrice → optionAmount")
    val optionAmount: Long,
    val state: V2MyTicketState,
    @field:Schema(description = "BEFORE / DONE / CANCELED")
    val entrance: V2EntranceState,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val enteredAt: LocalDateTime?,
    @field:Schema(description = "NONE / PENDING(선물 대기) / SENT(선물 완료) / RECEIVED(받은 티켓)")
    val giftState: V2GiftState,
    @field:Schema(description = "선물 대기 중인데 공연이 끝남 (선물 만료 — 회수만 가능)")
    val isGiftExpired: Boolean,
    @field:Schema(description = "받은 티켓 (소유자 = 나, 주문자 ≠ 나)")
    val isReceived: Boolean,
    @field:Schema(description = "PENDING·SENT·RECEIVED 의 선물 id (G-2 회수·G-8 메모 수정)")
    val giftId: Long?,
)

/**
 * T-3 티켓탭 공지 바 "NEW 티켓 주문이 승인되었습니다!" (8-4 A8).
 * 기준: 안 읽은 승인 알림(ORDER_APPROVED) 중 주문이 지금도 승인 상태인 것. 해제: 그 알림을 읽거나(N-3), 그 주문의 티켓을 T-2 로 열면 해제
 */
data class V2NewApprovedResponse(
    @field:Schema(description = "공지 바를 보일지")
    val hasNew: Boolean,
    @field:Schema(description = "새로 승인된 주문 uuid (최근 승인 알림 순). '티켓 보러가기'는 첫 번째 주문 묶음으로")
    val orderUuids: List<String>,
)

/** T-2 티켓 상세 + 입장 QR (내가 지금 소유한 티켓). 보낸 사람의 선물 완료 티켓(`GET /me/gifts/{giftId}/ticket`)도 같은 형태 — uuid·QR 없음 */
data class V2MyTicketDetailResponse(
    @field:Schema(description = "티켓 uuid. 선물 완료(SENT) 상세는 null")
    val ticketUuid: String?,
    val issuedTicketNo: String?,
    val ticketName: String?,
    @field:Schema(description = "티켓 1장 가격(원, 발급 시점). #755: ticketPrice → unitPrice")
    val unitPrice: Long,
    val optionAnswers: List<V2OptionAnswerResponse>,
    val optionAmount: Long,
    val state: V2MyTicketState,
    val entrance: V2EntranceState,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val enteredAt: LocalDateTime?,
    val giftState: V2GiftState,
    val isGiftExpired: Boolean,
    val isReceived: Boolean,
    @field:Schema(description = "입장 QR 값 (= 티켓 uuid). 선물 대기·취소 티켓은 null. 선물 수락·반환 때 바뀐다 (옛 값 무효)")
    val qrValue: String?,
    val event: V2MyOrderEventResponse,
    @field:Schema(description = "주문 uuid·예매 번호 (내 주문일 때만, 받은 티켓은 null)")
    val orderUuid: String?,
    val orderNo: String?,
    @field:Schema(description = "선물 정보 — 선물 대기(링크·메모) 또는 받은 티켓(보낸 사람)")
    val gift: V2TicketGiftInfoResponse?,
    @field:Schema(description = "지금 선물(G-1)할 수 있는지")
    val canGift: Boolean,
    @field:Schema(description = "지금 반환(G-6)할 수 있는지 (받은 티켓, 입장 전, 공연 시작 전)")
    val canReturn: Boolean,
)

data class V2TicketGiftInfoResponse(
    val giftId: Long,
    val status: TicketGiftStatus,
    @field:Schema(description = "선물 링크 토큰·경로 (선물 대기 중, 보낸 사람에게만)")
    val giftToken: String?,
    val linkPath: String?,
    @field:Schema(description = "메모 (보낸 사람에게만)")
    val memo: String?,
    @field:Schema(description = "받은 사람 닉네임 (선물 완료, 보낸 사람이 볼 때)")
    val receiverName: String?,
    @field:Schema(description = "보낸 사람 닉네임 (받은 티켓)")
    val senderName: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val createdAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val acceptedAt: LocalDateTime?,
)

// ===== 선물 G-1 ~ G-8 =====

/** G-1 결과 */
data class V2GiftCreatedResponse(
    val giftId: Long,
    @field:Schema(description = "선물 링크 토큰 (base64url 43자, 추측 불가). 링크를 가진 사람이 받는다")
    val giftToken: String,
    @field:Schema(description = "프론트 경로 (origin 은 프론트가 붙인다)", example = "/gifts/abc")
    val linkPath: String,
    val memo: String?,
)

/** G-2·G-4·G-5·G-6·G-8 결과 */
data class V2GiftResultResponse(
    val giftId: Long,
    val status: TicketGiftStatus,
    val cancelReason: TicketGiftCancelReason?,
    @field:Schema(description = "메모 (보낸 사람 요청에만)")
    val memo: String?,
    @field:Schema(description = "G-4 수락·G-6 반환 뒤 새 소유자의 티켓 uuid (수락이면 받은 사람의 새 QR). 그 밖 null")
    val ticketUuid: String?,
)

/** G-3 선물 랜딩 (비로그인 가능). 대기 중이 아니면 상태만 */
data class V2GiftLandingResponse(
    val status: TicketGiftStatus,
    @field:Schema(description = "AVAILABLE / ALREADY_ACCEPTED / REJECTED / RETURNED / CANCELED / OWN_LINK / EXPIRED (판정: 선물 상태 → 만료 → 본인 링크 → 받기 가능)")
    val viewState: V2GiftViewState,
    @field:Schema(description = "로그인했는지 (AVAILABLE 인데 false 면 로그인 유도)")
    val isLoggedIn: Boolean,
    @field:Schema(description = "ALREADY_ACCEPTED 일 때 보는 사람이 받은 본인인지")
    val isReceiver: Boolean,
    @field:Schema(description = "보는 사람이 보낸 사람이면 선물 id (회수 버튼용)")
    val giftId: Long?,
    @field:Schema(description = "보낸 사람 닉네임, 가운데를 가림 (예: 김*수, 김*). 대기 중일 때만 (기본안 — 공개 링크라 원문을 주지 않는다)")
    val senderName: String?,
    @field:Schema(description = "공연 요약 (대기 중일 때만)")
    val event: V2GiftEventResponse?,
    @field:Schema(description = "티켓 요약 (대기 중일 때만). 옵션 답변은 주지 않는다 (공개 링크 — 보낸 사람이 입력한 개인 정보일 수 있음)")
    val ticket: V2GiftTicketResponse?,
)

data class V2GiftEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val startAt: LocalDateTime?,
    val placeName: String?,
)

data class V2GiftTicketResponse(
    val ticketName: String?,
    @field:Schema(description = "티켓 1장 가격(원, 발급 시점). #755: ticketPrice → unitPrice")
    val unitPrice: Long,
)

/** G-7 보낸·받은 선물 내역 */
data class V2GiftElement(
    val giftId: Long,
    val status: TicketGiftStatus,
    val cancelReason: TicketGiftCancelReason?,
    @field:Schema(description = "대기 중인데 공연이 끝남 (선물 만료)")
    val isGiftExpired: Boolean,
    @field:Schema(description = "상대 닉네임: SENT 면 받은 사람(대기·취소면 null), RECEIVED 면 보낸 사람")
    val counterpartName: String?,
    @field:Schema(description = "메모 (SENT 만)")
    val memo: String?,
    @field:Schema(description = "선물 링크 토큰·경로 (SENT + 대기 중만)")
    val giftToken: String?,
    val linkPath: String?,
    val event: V2MyOrderEventResponse?,
    val ticketName: String?,
    val issuedTicketNo: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val createdAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val acceptedAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val rejectedAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val returnedAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val canceledAt: LocalDateTime?,
)
