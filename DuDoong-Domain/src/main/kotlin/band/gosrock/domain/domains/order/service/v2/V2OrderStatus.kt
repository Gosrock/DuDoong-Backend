package band.gosrock.domain.domains.order.service.v2

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.QOrder.order
import com.querydsl.core.types.dsl.BooleanExpression

/**
 * v2 호스트 주문 관리 상태 (#712). v1 [OrderStatus] 는 그대로 두고 조회 시 분류한다 (10문서 5-2, v1 상태값 유지).
 *
 * - 거절(REFUSED) = CANCELED 중 v2 거절(refuse_reason_type 있음) 또는 v1 거절(approved_at 없음).
 *   v1 의 취소(cancel)는 승인·결제 완료(APPROVED/CONFIRM, approved_at 기록) 주문만 가능하므로 approved_at 이 없는 CANCELED 는 승인 대기에서 거절된 주문이다
 *   (prod 2026-10-04: CANCELED 중 approved_at NULL 2,957건 전부 APPROVAL 주문)
 * - 취소(CANCELED) = 승인 후 호스트 취소(CANCELED + approved_at) + 사용자 환불(REFUND)
 * - 주문 실패(FAILED) = FAILED + OUTDATED
 * - 목록에서 제외: READY, PENDING_PAYMENT (결제 진행 중·이탈한 주문. 만료 기준 없음, DEC-004)
 *
 * [predicate] (목록·건수 쿼리) 와 [of] (응답 표시) 는 같은 기준이어야 한다.
 */
enum class V2OrderStatus {
    PENDING_APPROVE,
    APPROVED,
    REFUSED,
    CANCELED,
    FAILED;

    fun predicate(): BooleanExpression = when (this) {
        PENDING_APPROVE -> order.orderStatus.eq(OrderStatus.PENDING_APPROVE)
        APPROVED -> order.orderStatus.`in`(OrderStatus.APPROVED, OrderStatus.CONFIRM)
        REFUSED -> order.orderStatus.eq(OrderStatus.CANCELED)
            .and(order.refuseReasonType.isNotNull.or(order.approvedAt.isNull))
        CANCELED -> order.orderStatus.eq(OrderStatus.CANCELED)
            .and(order.refuseReasonType.isNull).and(order.approvedAt.isNotNull)
            .or(order.orderStatus.eq(OrderStatus.REFUND))
        FAILED -> order.orderStatus.`in`(OrderStatus.FAILED, OrderStatus.OUTDATED)
    }

    companion object {
        /** v2 목록에 보이는 주문 전체 (= 다섯 상태의 합) */
        fun visiblePredicate(): BooleanExpression = order.orderStatus.notIn(OrderStatus.READY, OrderStatus.PENDING_PAYMENT)

        /** 목록에서 제외되는 주문(READY, PENDING_PAYMENT)은 null */
        fun of(order: Order): V2OrderStatus? = when (order.orderStatus) {
            OrderStatus.PENDING_APPROVE -> PENDING_APPROVE
            OrderStatus.APPROVED, OrderStatus.CONFIRM -> APPROVED
            OrderStatus.CANCELED -> if (order.refuseReasonType != null || order.approvedAt == null) REFUSED else CANCELED
            OrderStatus.REFUND -> CANCELED
            OrderStatus.FAILED, OrderStatus.OUTDATED -> FAILED
            OrderStatus.READY, OrderStatus.PENDING_PAYMENT -> null
        }
    }
}
