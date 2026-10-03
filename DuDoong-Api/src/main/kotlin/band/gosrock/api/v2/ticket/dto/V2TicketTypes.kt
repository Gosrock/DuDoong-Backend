package band.gosrock.api.v2.ticket.dto

import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType

/** v2 티켓 결제 방식. 요청은 DUDOONG(계좌송금) / FREE 만 받고, PRICE(PG) 는 기존 티켓 조회용 */
enum class V2TicketPayType(val domain: TicketPayType) {
    DUDOONG(TicketPayType.DUDOONG_TICKET),
    FREE(TicketPayType.FREE_TICKET),
    PRICE(TicketPayType.PRICE_TICKET),
    ;

    companion object {
        fun of(payType: TicketPayType?): V2TicketPayType? = entries.firstOrNull { it.domain == payType }
    }
}

/** v2 옵션 응답 형식. YES_NO = v1 TRUE_FALSE. MULTIPLE_CHOICE 는 v1 데이터 조회용 (v2 생성 불가) */
enum class V2TicketOptionType(val domain: OptionGroupType) {
    SUBJECTIVE(OptionGroupType.SUBJECTIVE),
    YES_NO(OptionGroupType.TRUE_FALSE),
    MULTIPLE_CHOICE(OptionGroupType.MULTIPLE_CHOICE),
    ;

    companion object {
        fun of(type: OptionGroupType?): V2TicketOptionType? = entries.firstOrNull { it.domain == type }
    }
}
