package band.gosrock.api.v2.operation.dto.response

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.operation.dto.V2RefundStatus
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.service.v2.V2OrderCounts
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** R-1 주문 목록 + 상태별 건수 */
data class V2OrderListResponse(
    @field:Schema(description = "상태별 건수 (검색어 반영, 상태 필터 무시)")
    val counts: V2OrderCountsResponse,
    val orders: V2PageResponse<V2OrderElement>,
)

data class V2OrderCountsResponse(
    val all: Long,
    val pendingApprove: Long,
    val approved: Long,
    val refused: Long,
    val canceled: Long,
    val failed: Long,
) {
    companion object {
        fun of(counts: V2OrderCounts) = V2OrderCountsResponse(
            all = counts.all,
            pendingApprove = counts.pendingApprove,
            approved = counts.approved,
            refused = counts.refused,
            canceled = counts.canceled,
            failed = counts.failed,
        )
    }
}

data class V2OrderElement(
    val orderUuid: String,
    @field:Schema(description = "주문 번호 (R1000xxxx)")
    val orderNo: String?,
    val buyerName: String?,
    @field:Schema(description = "주문자 연락처 010-xxxx-xxxx (마스킹 없음: 입금 확인·연락용, v1 어드민 목록과 같은 범위)")
    val buyerPhone: String?,
    @field:Schema(description = "티켓 이름 (주문 이름)")
    val ticketName: String?,
    val totalQuantity: Long,
    @field:Schema(description = "총 결제금액(원)")
    val totalPaymentAmount: Long,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val orderedAt: LocalDateTime?,
    @field:Schema(description = "PENDING_APPROVE / APPROVED / REFUSED / CANCELED / FAILED")
    val status: V2OrderStatus?,
    @field:Schema(description = "NONE / REQUESTED(환불 요청) / COMPLETED(환불 완료)")
    val refundStatus: V2RefundStatus,
    @field:Schema(description = "거절 사유 종류. v1 에서 거절한 주문은 null (사유 문구는 refuseReason)")
    val refuseReasonType: OrderRefuseReasonType?,
    @field:Schema(description = "거절 사유 문구 (REFUSED 일 때만)")
    val refuseReason: String?,
    @field:Schema(description = "취소 사유 (CANCELED 일 때만)")
    val cancelReason: String?,
)

/** R-2 주문 상세 */
data class V2OrderDetailResponse(
    val order: V2OrderElement,
    @field:Schema(description = "주문자 이메일 (상세에서만)")
    val buyerEmail: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val approvedAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "거절·취소 시각")
    val withdrawnAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val refundStatusChangedAt: LocalDateTime?,
    @field:Schema(description = "주문 라인 (승인 전에도 옵션 응답 확인용)")
    val lines: List<V2OrderLineResponse>,
    @field:Schema(description = "발급 티켓별 옵션 응답 (승인 후). 거절·승인 대기 주문은 빈 목록")
    val issuedTickets: List<V2OrderIssuedTicketResponse>,
)

data class V2OrderLineResponse(
    val ticketItemId: Long?,
    val ticketName: String?,
    val unitPrice: Long,
    val quantity: Long,
    @field:Schema(description = "(티켓 가격 + 옵션 추가금) x 수량")
    val linePrice: Long,
    val optionAnswers: List<V2OptionAnswerResponse>,
)

data class V2OrderIssuedTicketResponse(
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
)

data class V2OptionAnswerResponse(
    val optionName: String?,
    val answer: String?,
    val additionalPrice: Long,
)

/** F-1 환불 목록 / F-2 결과 */
data class V2RefundElement(
    val orderUuid: String?,
    val orderNo: String?,
    val buyerName: String?,
    val ticketName: String?,
    val totalPaymentAmount: Long,
    val status: V2OrderStatus?,
    val refundStatus: V2RefundStatus,
    @field:Schema(description = "거절·취소 사유 문구 (cancel_reason)")
    val reason: String?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val withdrawnAt: LocalDateTime?,
    @field:DateFormat
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm")
    val refundStatusChangedAt: LocalDateTime?,
)
