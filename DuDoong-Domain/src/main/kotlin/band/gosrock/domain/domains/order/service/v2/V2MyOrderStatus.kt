package band.gosrock.domain.domains.order.service.v2

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.QOrder.order
import band.gosrock.domain.domains.order.domain.RefundStatus
import com.querydsl.core.types.dsl.BooleanExpression

/**
 * 사용자 앱 내 주문 상태 (#718, O-2·O-3). v1 [OrderStatus] 는 그대로 두고 조회 시 분류한다.
 * 호스트 분류([V2OrderStatus])와 거절·승인 기준은 같고, 사용자 철회(REFUND)를 환불 여부로 나눈다.
 *
 * - 거절(REFUSED) = [V2OrderStatus.REFUSED] 와 같음
 * - 취소(CANCELED) = 승인 후 호스트 취소(CANCELED + approved_at, 사유 종류 없음) + 환불할 돈 없는 사용자 취소(REFUND + 환불 상태 NONE: v2 무료 주문 취소)
 * - 환불(REFUNDED) = 사용자 취소·환불 요청(REFUND + 환불 요청/완료). 요청·완료 구분은 refundStatus 로 본다
 * - 목록 제외: READY, PENDING_PAYMENT(결제 진행 중·이탈), FAILED, OUTDATED — v1 마이페이지 주문 목록과 같은 기준
 *
 * [predicate] 와 [of] 는 같은 기준이어야 한다.
 */
enum class V2MyOrderStatus {
    PENDING_APPROVE,
    APPROVED,
    REFUSED,
    CANCELED,
    REFUNDED;

    fun predicate(): BooleanExpression = when (this) {
        PENDING_APPROVE -> V2OrderStatus.PENDING_APPROVE.predicate()
        APPROVED -> V2OrderStatus.APPROVED.predicate()
        REFUSED -> V2OrderStatus.REFUSED.predicate()
        CANCELED -> order.orderStatus.eq(OrderStatus.CANCELED)
            .and(order.refuseReasonType.isNull).and(order.approvedAt.isNotNull)
            .or(order.orderStatus.eq(OrderStatus.REFUND).and(order.refundStatus.eq(RefundStatus.NONE)))
        REFUNDED -> order.orderStatus.eq(OrderStatus.REFUND).and(order.refundStatus.ne(RefundStatus.NONE))
    }

    companion object {
        private val HIDDEN = listOf(OrderStatus.READY, OrderStatus.PENDING_PAYMENT, OrderStatus.FAILED, OrderStatus.OUTDATED)

        /** 목록에 보이는 주문 전체 (= 다섯 상태의 합) */
        fun visiblePredicate(): BooleanExpression = order.orderStatus.notIn(HIDDEN)

        /** 목록에서 제외되는 주문(결제 진행 중·실패)은 null */
        fun of(order: Order): V2MyOrderStatus? = when (order.orderStatus) {
            OrderStatus.REFUND -> if (order.refundStatus == RefundStatus.NONE) CANCELED else REFUNDED
            in HIDDEN -> null
            else -> when (V2OrderStatus.of(order)) {
                V2OrderStatus.PENDING_APPROVE -> PENDING_APPROVE
                V2OrderStatus.APPROVED -> APPROVED
                V2OrderStatus.REFUSED -> REFUSED
                V2OrderStatus.CANCELED -> CANCELED
                V2OrderStatus.FAILED, null -> null
            }
        }
    }
}
