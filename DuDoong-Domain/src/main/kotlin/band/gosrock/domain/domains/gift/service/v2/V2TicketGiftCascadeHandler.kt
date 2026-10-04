package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.domain.common.events.event.EventAdminStatusChangeEvent
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.common.events.user.UserDeactivatedEvent
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 선물 연쇄 처리 (#719, DEC-026 #8·#9·#10, 8-4 A5). v1·v2·운영 어드민 어느 경로든 도메인 엔티티의 전이가 내는 이벤트에 붙는다:
 * - [WithDrawOrderEvent]: `Order.cancel/refuse/refund/withdrawByUser` — v1·v2 호스트 취소, 운영 취소, 사용자 환불·취소, 거절
 * - [UserDeactivatedEvent]: `User.withDrawUser/changeAccountState` — 회원 탈퇴, 운영 사용자 상태 변경(정지·탈퇴)
 * - [EventAdminStatusChangeEvent]: `Event.adminUpdateStatus` — 운영 공연 삭제·상태 변경
 *
 * **BEFORE_COMMIT** (원 트랜잭션 안)에서 처리한다: 연쇄가 실패하면 원 전이도 롤백되어 "주문은 취소됐는데 링크는 살아 있음" 같은 상태가 커밋되지 않는다.
 * AFTER_COMMIT 이면 원 전이 커밋과 연쇄 사이에 수락이 끼어들 수 있고 연쇄 실패를 되돌릴 수 없다.
 * 주문 연쇄는 v1 티켓 철회 핸들러(같은 이벤트, BEFORE_COMMIT)보다 먼저 돌아 티켓 행을 먼저 잠근다.
 * 알림(선물받은 티켓 취소)은 커밋 후 알림 핸들러가 따로 저장한다.
 */
@Component
class V2TicketGiftCascadeHandler(
    private val giftDomainService: V2TicketGiftDomainService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
    fun handleWithDrawOrder(event: WithDrawOrderEvent) {
        val canceled = giftDomainService.cancelPendingByOrder(event.orderUuid)
        if (canceled > 0) log.info("[선물] 주문 철회로 대기 선물 {}건 취소 orderUuid={}", canceled, event.orderUuid)
    }

    @TransactionalEventListener(classes = [UserDeactivatedEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
    fun handleUserDeactivated(event: UserDeactivatedEvent) {
        val canceled = giftDomainService.cancelPendingBySender(event.userId)
        if (canceled > 0) log.info("[선물] 보낸 사람 {} 로 대기 선물 {}건 취소 userId={}", event.accountState, canceled, event.userId)
    }

    @TransactionalEventListener(classes = [EventAdminStatusChangeEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
    fun handleEventAdminStatusChange(event: EventAdminStatusChangeEvent) {
        val canceled = giftDomainService.cancelPendingByEventRemoved(event.eventId, event.status)
        if (canceled > 0) log.info("[선물] 공연 운영 상태 {} 로 대기 선물 {}건 취소 eventId={}", event.status, canceled, event.eventId)
    }
}
