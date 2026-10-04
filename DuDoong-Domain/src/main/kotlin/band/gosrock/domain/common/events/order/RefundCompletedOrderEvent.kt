package band.gosrock.domain.common.events.order

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

/**
 * 환불 완료 처리됨 (#726). [band.gosrock.domain.domains.order.domain.Order.completeRefund] 에서 발행하므로 v1 호스트 환불 완료·v2 F-2·운영 어드민 경로 모두 같다.
 * 지금 받는 곳은 v2 알림센터뿐이다 (사용자에게 환불 완료 알림). v1 운영 어드민은 주문 상태와 무관하게 환불 완료로 바꿀 수 있어,
 * 이벤트는 주문 uuid 만 담고 대상 판정(결제 방식·금액)은 알림 서비스가 커밋된 주문으로 한다
 */
class RefundCompletedOrderEvent(val orderUuid: String) : DomainEvent()
