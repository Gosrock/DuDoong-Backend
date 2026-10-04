package band.gosrock.api.v2.notification.handler

import band.gosrock.api.v2.notification.handler.V2NotificationAsyncConfig.Companion.NOTIFICATION_EXECUTOR
import band.gosrock.domain.common.events.order.CreateOrderEvent
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.host.service.v2.V2HostMembersAddedEvent
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * v2 알림센터 저장 핸들러 (#714). 슬랙·알림톡 핸들러와 같은 이벤트에 하나 더 붙는다 (v1 경로에서 발생해도 저장).
 *
 * - 원 트랜잭션 커밋 후(AFTER_COMMIT) 알림 전용 executor([V2NotificationAsyncConfig])에서 실행하고, 저장은 [V2NotificationDomainService] 의 새 트랜잭션에서 한다.
 *   원 트랜잭션이 롤백되면 호출되지 않고, 알림 저장이 실패해도 주문·승인·멤버 추가는 이미 커밋돼 있다.
 * - `condition`(SpEL)은 비동기 큐에 넣기 전에 평가된다. 이벤트 필드만으로 알 수 있는 비대상(결제형 주문, 카드 결제 환불)은 큐에 넣지 않는다.
 *   나머지 판정(주문 상태, 거절/취소 구분, 수신자)은 커밋된 데이터를 읽는 서비스가 한다.
 * - 예외는 여기서 삼키고 로그만 남긴다 (재시도 없음 — 알림은 보조 기능)
 */
@Component
class V2NotificationEventHandler(
    private val notificationDomainService: V2NotificationDomainService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [V2HostMembersAddedEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleHostMembersAdded(event: V2HostMembersAddedEvent) =
        save("HOST_MEMBER_ADDED", event.toString()) { notificationDomainService.notifyHostMembersAdded(event.hostId, event.userIds) }

    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [CreateOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT, condition = APPROVAL_ORDER)
    fun handleCreateOrder(event: CreateOrderEvent) =
        save("ORDER_PENDING_APPROVE", event.orderUuid) { notificationDomainService.notifyOrderPendingApprove(event.orderUuid) }

    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [DoneOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT, condition = APPROVAL_ORDER)
    fun handleDoneOrder(event: DoneOrderEvent) =
        save("ORDER_APPROVED", event.orderUuid) { notificationDomainService.notifyOrderApproved(event.orderUuid) }

    /** 거절·승인 후 취소(둘 다 CANCELED)·환불(REFUND)이 모두 이 이벤트. 환불은 condition 으로, 거절/취소 구분은 서비스가 커밋된 주문으로 판정 */
    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT, condition = APPROVAL_CANCELED_ORDER)
    fun handleWithDrawOrder(event: WithDrawOrderEvent) =
        save("ORDER_REFUSED", event.orderUuid) { notificationDomainService.notifyOrderRefused(event.orderUuid) }

    /** 사용자 취소·환불 요청(REFUND, v1 사용자 환불 포함) → 호스트 마스터·매니저 (#718). 카드(PG) 결제 주문은 condition 으로 거른다 */
    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT, condition = USER_WITHDRAWN_ORDER)
    fun handleUserWithdrawnOrder(event: WithDrawOrderEvent) =
        save("ORDER_REFUND_REQUESTED/ORDER_CANCELED_BY_USER", event.orderUuid) { notificationDomainService.notifyOrderWithdrawnByUser(event.orderUuid) }

    private fun save(type: String, key: String, block: () -> Int) {
        try {
            val saved = block()
            log.info("[알림센터] {} 저장 {}건 ({})", type, saved, key)
        } catch (e: Exception) {
            log.warn("[알림센터] {} 저장 실패 ({}): {}", type, key, e.message, e)
        }
    }

    companion object {
        /** `#p0` = 리스너 첫 번째 인자(이벤트). 승인형 주문만 */
        const val APPROVAL_ORDER = "#p0.orderMethod.name() == 'APPROVAL'"

        /** 승인형 + CANCELED (거절 또는 승인 후 취소). REFUND(사용자 환불)는 제외 */
        const val APPROVAL_CANCELED_ORDER = "#p0.orderMethod.name() == 'APPROVAL' and #p0.orderStatus.name() == 'CANCELED'"

        /** 사용자 철회(REFUND) 중 승인형이거나 결제할 돈이 없던 주문 (paymentKey 는 결제가 필요한 주문에만 있다 = 카드 결제는 제외) */
        const val USER_WITHDRAWN_ORDER = "#p0.orderStatus.name() == 'REFUND' and (#p0.orderMethod.name() == 'APPROVAL' or #p0.paymentKey == null)"
    }
}
