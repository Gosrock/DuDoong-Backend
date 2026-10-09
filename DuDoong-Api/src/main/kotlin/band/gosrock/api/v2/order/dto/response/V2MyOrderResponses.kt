package band.gosrock.api.v2.order.dto.response

import band.gosrock.api.v2.operation.dto.V2RefundStatus
import band.gosrock.api.v2.operation.dto.response.V2OptionAnswerResponse
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import band.gosrock.domain.domains.gift.service.v2.V2GiftState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.service.v2.V2MyOrderStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 주문의 공연 요약 */
data class V2MyOrderEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val startAt: LocalDateTime?,
    @field:Schema(description = "PREPARING / UPCOMING / ONGOING / PAST (호스팅 센터·공연 탐색과 같은 판정)")
    val displayStatus: V2EventDisplayStatus,
)

/** O-2 내 주문 목록 항목 */
data class V2MyOrderElement(
    val orderUuid: String,
    @field:Schema(description = "예매 번호 (R1000xxxx)")
    val orderNo: String?,
    val event: V2MyOrderEventResponse?,
    @field:Schema(description = "티켓 이름 (주문 이름)")
    val ticketName: String?,
    val quantity: Long,
    @field:Schema(description = "총 결제금액(원)")
    val totalAmount: Long,
    @field:Schema(description = "PENDING_APPROVE / APPROVED / REFUSED / CANCELED / REFUNDED")
    val status: V2MyOrderStatus?,
    @field:Schema(description = "NONE / REQUESTED(환불 요청) / COMPLETED(환불 완료)")
    val refundStatus: V2RefundStatus,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val orderedAt: LocalDateTime?,
)

/** O-3 내 주문 상세 (O-1·O-4 응답도 같음) */
data class V2MyOrderDetailResponse(
    val orderUuid: String,
    val orderNo: String?,
    val status: V2MyOrderStatus?,
    val refundStatus: V2RefundStatus,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val orderedAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val approvedAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "거절·취소·환불 요청 시각")
    val withdrawnAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "환불 상태가 바뀐 시각 (요청·완료)")
    val refundStatusChangedAt: LocalDateTime?,
    val event: V2MyOrderEventResponse?,
    val ticket: V2MyOrderTicketResponse,
    @field:Schema(description = "주문 시점의 승인형 여부 (#740): true = 호스트 승인 후 발급(두둥티켓·무료 승인형), false = 바로 확정(무료 선착순·PG). 티켓 설정이 나중에 바뀌어도 그대로")
    val approvalRequired: Boolean,
    val quantity: Long,
    @field:Schema(description = "BANK_TRANSFER / TOSS_TRANSFER / FREE. v1 에서 만든 주문은 null")
    val paymentChannel: OrderPaymentChannel?,
    @field:Schema(description = "입금자명 (v2 두둥티켓 주문)")
    val depositorName: String?,
    @field:Schema(description = "결제 금액·입금 계좌. **유료 주문만** (무료는 null). 토스 송금 딥링크는 프론트가 이 값으로 만든다")
    val payment: V2MyOrderPaymentResponse?,
    @field:Schema(description = "주문 라인별 옵션 답변 (티켓별 입력이면 수량 1 라인 N개). 선물 상태는 티켓 단위라 `issuedTickets` 에만 있다 (#752)")
    val lines: List<V2MyOrderLineResponse>,
    @field:Schema(description = "거절 사유 종류 (REFUSED, v2 거절만). DEPOSIT_UNCONFIRMED / AMOUNT_MISMATCH / SOLD_OUT / ETC")
    val refuseReasonType: OrderRefuseReasonType?,
    @field:Schema(description = "거절 사유 문구 (REFUSED 일 때)")
    val refuseReason: String?,
    @field:Schema(description = "호스트 취소 사유 (승인 후 호스트가 취소한 경우)")
    val cancelReason: String?,
    @field:Schema(description = "입력한 환불 계좌 (계좌번호 뒤 4자리만). O-4 취소 때 또는 환불 계좌 입력 API 로 입력")
    val refundAccount: V2MyRefundAccountResponse?,
    @field:Schema(description = "지금 환불 계좌를 입력·수정할 수 있는지 (#728): v2 주문 중 환불 요청 중인 유료 계좌이체 주문(거절·호스트 취소·사용자 취소), 환불 완료 전. v1 주문은 false (O-4 로 계좌를 입력한 v1 주문은 수정만 가능해 true)")
    val refundAccountEditable: Boolean = false,
    @field:Schema(description = "환불 계좌 입력이 필요한지 (#728): 입력 가능한데 아직 계좌가 없음 — 호스트 거절·취소 뒤 주문상세에서 입력 유도. 입금 미확인 거절은 false (입력은 가능)")
    val refundAccountRequired: Boolean = false,
    @field:Schema(description = "발급 티켓: 본인 소유분 + 내가 선물해 수락된 티켓(giftState=SENT, ticketUuid null)")
    val issuedTickets: List<V2MyOrderIssuedTicketResponse>,
    @field:Schema(description = "지금 취소(O-4)할 수 있는지. 승인 대기: 공연 시작 전, 승인 완료: 공연 시작 전 + 입장·선물 대기·선물 완료 티켓 없음")
    val canCancel: Boolean,
)

data class V2MyOrderTicketResponse(
    val ticketItemId: Long?,
    val name: String?,
    val payType: V2TicketPayType?,
    @field:Schema(description = "티켓 1장 가격(원, 주문 시점)")
    val unitPrice: Long,
)

data class V2MyOrderPaymentResponse(
    @field:Schema(description = "티켓 금액 합 (티켓 가격 x 수량)")
    val ticketAmount: Long,
    @field:Schema(description = "옵션 추가금 합")
    val optionAmount: Long,
    @field:Schema(description = "할인 (v1 쿠폰 주문만, v2 는 0)")
    val discountAmount: Long,
    @field:Schema(description = "총 결제금액 = 티켓 + 옵션 - 할인")
    val totalAmount: Long,
    @field:Schema(description = "입금 계좌 (두둥티켓). 카드 결제(v1 PG) 주문은 null")
    val account: V2MyOrderAccountResponse?,
)

data class V2MyOrderAccountResponse(
    val bankName: String?,
    val accountHolder: String?,
    val accountNumber: String?,
)

data class V2MyOrderLineResponse(
    val quantity: Long,
    @field:Schema(description = "(티켓 가격 + 옵션 추가금) x 수량")
    val linePrice: Long,
    val optionAnswers: List<V2OptionAnswerResponse>,
)

data class V2MyRefundAccountResponse(
    val bankName: String,
    val accountHolder: String,
    @field:Schema(description = "뒤 4자리만 (예: ******6789)")
    val maskedAccountNumber: String,
)

data class V2MyOrderIssuedTicketResponse(
    @field:Schema(description = "티켓 uuid. 선물 완료(SENT) 행은 null (받은 사람의 QR)")
    val ticketUuid: String?,
    @field:Schema(description = "티켓 번호 (T1000xxxx)")
    val issuedTicketNo: String?,
    val ticketName: String?,
    @field:Schema(description = "BEFORE / DONE / CANCELED")
    val entrance: V2EntranceState,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val enteredAt: LocalDateTime?,
    val optionAnswers: List<V2OptionAnswerResponse>,
    @field:Schema(description = "NONE / PENDING(선물 대기중) / SENT(선물 완료). 주문상세는 주문자만 보므로 RECEIVED 는 없음 (#719)")
    val giftState: V2GiftState = V2GiftState.NONE,
    @field:Schema(description = "선물 대기 중인데 공연이 끝남 (선물 만료)")
    val isGiftExpired: Boolean = false,
    @field:Schema(description = "PENDING·SENT 의 선물 id")
    val giftId: Long? = null,
)
