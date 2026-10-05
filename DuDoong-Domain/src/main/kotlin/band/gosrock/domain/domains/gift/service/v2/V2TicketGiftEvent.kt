package band.gosrock.domain.domains.gift.service.v2

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

/** 선물 알림이 필요한 전이 (#719, 11 문서 8-2 알림 표). 보낸 사람 회수(G-2)와 연쇄 취소는 발행하지 않는다 */
enum class V2TicketGiftChange {
    /** G-1 → 보낸 사람 GIFT_SENT */
    SENT,

    /** G-4 → 보낸 사람 GIFT_ACCEPTED, 받은 사람 GIFT_RECEIVED */
    ACCEPTED,

    /** G-5 → 보낸 사람 GIFT_REJECTED */
    REJECTED,

    /** G-6 → 보낸 사람 GIFT_RETURNED */
    RETURNED,
}

/** v2 선물 전이 이벤트. `V2TicketGiftDomainService` 에서만 발행하고, 알림 저장 핸들러가 커밋 후 비동기로 받는다 */
class V2TicketGiftEvent(
    val giftId: Long,
    val change: V2TicketGiftChange,
) : DomainEvent() {
    override fun toString(): String = "V2TicketGiftEvent(giftId=$giftId, change=$change)"
}
