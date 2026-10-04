package band.gosrock.api.v2.order.dto

import band.gosrock.domain.domains.order.service.v2.V2MyOrderStatus

/** O-2 상태 필터. ALL = 보이는 주문 전체 (결제 진행 중·실패 제외) */
enum class V2MyOrderStatusFilter(val domain: V2MyOrderStatus?) {
    ALL(null),
    PENDING_APPROVE(V2MyOrderStatus.PENDING_APPROVE),
    APPROVED(V2MyOrderStatus.APPROVED),
    REFUSED(V2MyOrderStatus.REFUSED),
    CANCELED(V2MyOrderStatus.CANCELED),
    REFUNDED(V2MyOrderStatus.REFUNDED),
}
