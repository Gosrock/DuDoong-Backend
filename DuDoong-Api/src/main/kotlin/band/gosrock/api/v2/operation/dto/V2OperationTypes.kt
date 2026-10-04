package band.gosrock.api.v2.operation.dto

import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus

/** R-1 / R-6 상태 필터. ALL = 목록에 보이는 주문 전체 (결제 진행 중 READY·PENDING_PAYMENT 제외) */
enum class V2OrderStatusFilter(val domain: V2OrderStatus?) {
    ALL(null),
    PENDING_APPROVE(V2OrderStatus.PENDING_APPROVE),
    APPROVED(V2OrderStatus.APPROVED),
    REFUSED(V2OrderStatus.REFUSED),
    CANCELED(V2OrderStatus.CANCELED),
    FAILED(V2OrderStatus.FAILED),
}

/** I-1 / I-3 입장 필터. ALL = 유효(취소 제외) 티켓 전체 */
enum class V2EntranceFilter(val domain: V2EntranceState?) {
    ALL(null),
    BEFORE(V2EntranceState.BEFORE),
    DONE(V2EntranceState.DONE),
}

/** F-1 환불 상태 필터. null 이면 요청·완료 전부 */
enum class V2RefundStatusFilter(val domain: RefundStatus) {
    REQUESTED(RefundStatus.REFUND_REQUESTED),
    COMPLETED(RefundStatus.REFUND_COMPLETED),
}

/** 응답용 환불 상태 */
enum class V2RefundStatus {
    NONE,
    REQUESTED,
    COMPLETED;

    companion object {
        fun of(status: RefundStatus): V2RefundStatus = when (status) {
            RefundStatus.NONE -> NONE
            RefundStatus.REFUND_REQUESTED -> REQUESTED
            RefundStatus.REFUND_COMPLETED -> COMPLETED
        }
    }
}
