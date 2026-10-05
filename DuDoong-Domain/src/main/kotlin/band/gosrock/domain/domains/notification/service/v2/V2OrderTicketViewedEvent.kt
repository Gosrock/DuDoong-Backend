package band.gosrock.domain.domains.notification.service.v2

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

/**
 * 주문자가 그 주문의 티켓을 열었음 (T-2·G-7a, #719 T-3 공지 바 해제). 안 읽은 승인 알림이 있을 때만 발행한다.
 * 읽음 처리는 조회 트랜잭션이 끝난 뒤(AFTER_COMMIT) 알림 전용 풀에서 한다 — 조회가 커넥션을 하나만 쓰고, 읽음 처리 실패가 조회에 번지지 않게
 */
class V2OrderTicketViewedEvent(val userId: Long, val orderUuid: String) : DomainEvent() {
    override fun toString(): String = "V2OrderTicketViewedEvent(userId=$userId, orderUuid=$orderUuid)"
}
