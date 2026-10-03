package band.gosrock.api.v2.notification.handler

import band.gosrock.domain.common.events.order.CreateOrderEvent
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.host.service.v2.V2HostMembersAddedEvent
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.order.domain.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * v2 알림센터 저장 핸들러 (#714). 슬랙·알림톡 핸들러와 같은 이벤트에 하나 더 붙는다 (v1 경로에서 발생해도 저장).
 *
 * - 원 트랜잭션 커밋 후(AFTER_COMMIT) 별도 스레드(@Async)에서 실행하고, 저장은 [V2NotificationDomainService] 의 새 트랜잭션에서 한다.
 *   원 트랜잭션이 롤백되면 호출되지 않고, 알림 저장이 실패해도 주문·승인·멤버 추가는 이미 커밋돼 있다.
 * - 예외는 여기서 삼키고 로그만 남긴다 (재시도 없음 — 알림은 보조 기능)
 */
@Component
class V2NotificationEventHandler(
    private val notificationDomainService: V2NotificationDomainService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(classes = [V2HostMembersAddedEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleHostMembersAdded(event: V2HostMembersAddedEvent) =
        save("HOST_MEMBER_ADDED", event.toString()) { notificationDomainService.notifyHostMembersAdded(event.hostId, event.userIds) }

    @Async
    @TransactionalEventListener(classes = [CreateOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleCreateOrder(event: CreateOrderEvent) {
        if (event.orderMethod.isPayment()) return
        save("ORDER_PENDING_APPROVE", event.orderUuid) { notificationDomainService.notifyOrderPendingApprove(event.orderUuid) }
    }

    @Async
    @TransactionalEventListener(classes = [DoneOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleDoneOrder(event: DoneOrderEvent) {
        if (event.orderMethod.isPayment()) return
        save("ORDER_APPROVED", event.orderUuid) { notificationDomainService.notifyOrderApproved(event.orderUuid) }
    }

    /** 거절·승인 후 취소·환불이 모두 이 이벤트. 거절 여부는 커밋된 주문으로 서비스가 판정한다 (환불은 여기서 거른다) */
    @Async
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleWithDrawOrder(event: WithDrawOrderEvent) {
        if (event.orderMethod.isPayment() || event.orderStatus != OrderStatus.CANCELED) return
        save("ORDER_REFUSED", event.orderUuid) { notificationDomainService.notifyOrderRefused(event.orderUuid) }
    }

    private fun save(type: String, key: String, block: () -> Int) {
        try {
            val saved = block()
            log.info("[알림센터] {} 저장 {}건 ({})", type, saved, key)
        } catch (e: Exception) {
            log.warn("[알림센터] {} 저장 실패 ({}): {}", type, key, e.message, e)
        }
    }
}
