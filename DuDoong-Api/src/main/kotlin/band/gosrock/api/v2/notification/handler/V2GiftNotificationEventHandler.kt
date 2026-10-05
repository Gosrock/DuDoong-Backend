package band.gosrock.api.v2.notification.handler

import band.gosrock.api.v2.notification.handler.V2NotificationAsyncConfig.Companion.NOTIFICATION_EXECUTOR
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.gift.service.v2.V2TicketGiftChange
import band.gosrock.domain.domains.gift.service.v2.V2TicketGiftEvent
import band.gosrock.domain.domains.notification.service.v2.V2GiftNotificationDomainService
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.notification.service.v2.V2OrderTicketViewedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 선물 알림 저장 핸들러 (#719). T-3 공지 바 해제(티켓 열람 → 승인 알림 읽음)도 여기서 커밋 후 처리한다. [V2NotificationEventHandler] 와 같은 방식: 원 트랜잭션 커밋 후(AFTER_COMMIT) 알림 전용 executor 에서,
 * 저장은 [V2GiftNotificationDomainService] 의 새 트랜잭션. 예외는 삼키고 로그만 (선물 전이·주문 취소는 이미 커밋됨)
 */
@Component
class V2GiftNotificationEventHandler(
    private val giftNotificationDomainService: V2GiftNotificationDomainService,
    private val notificationDomainService: V2NotificationDomainService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [V2TicketGiftEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleGift(event: V2TicketGiftEvent) = save("GIFT_${event.change}", event.giftId.toString()) {
        when (event.change) {
            V2TicketGiftChange.SENT -> giftNotificationDomainService.notifyGiftSent(event.giftId)
            V2TicketGiftChange.ACCEPTED -> giftNotificationDomainService.notifyGiftAccepted(event.giftId)
            V2TicketGiftChange.REJECTED -> giftNotificationDomainService.notifyGiftRejected(event.giftId)
            V2TicketGiftChange.RETURNED -> giftNotificationDomainService.notifyGiftReturned(event.giftId)
        }
    }

    /** 호스트·운영 취소(CANCELED)로 선물받은 티켓이 취소됨 → 받은 사람. 사용자 환불(REFUND)은 선물 티켓이 있으면 막히므로 대상 아님 */
    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT, condition = CANCELED_ORDER)
    fun handleWithDrawOrder(event: WithDrawOrderEvent) =
        save("GIFT_TICKET_CANCELED", event.orderUuid) { giftNotificationDomainService.notifyGiftTicketsCanceled(event.orderUuid) }

    /** T-2·G-7a 로 주문 티켓을 연 뒤 그 주문의 승인 알림 읽음 처리 (T-3 공지 바 해제). 조회 커밋 후, 실패는 warn 만 */
    @Async(NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(classes = [V2OrderTicketViewedEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderTicketViewed(event: V2OrderTicketViewedEvent) =
        save("ORDER_APPROVED 읽음", event.orderUuid) { notificationDomainService.markOrderApprovedRead(event.userId, event.orderUuid) }

    private fun save(type: String, key: String, block: () -> Int) {
        try {
            val saved = block()
            if (saved > 0) log.info("[알림센터] {} 저장 {}건 ({})", type, saved, key)
        } catch (e: Exception) {
            log.warn("[알림센터] {} 저장 실패 ({}): {}", type, key, e.message, e)
        }
    }

    companion object {
        const val CANCELED_ORDER = "#p0.orderStatus.name() == 'CANCELED'"
    }
}
