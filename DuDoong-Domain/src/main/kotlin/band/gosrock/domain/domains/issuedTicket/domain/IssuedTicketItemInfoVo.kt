package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class IssuedTicketItemInfoVo(
    var ticketItemId: Long? = null,

    @Enumerated(EnumType.STRING)
    var ticketType: TicketType? = null,

    @Enumerated(EnumType.STRING)
    var payType: TicketPayType? = null,

    var ticketName: String? = null,

    var price: Money? = null,
) {
    companion object {
        @JvmStatic
        fun from(item: TicketItem): IssuedTicketItemInfoVo = IssuedTicketItemInfoVo(
            ticketItemId = item.id,
            ticketType = item.type,
            payType = item.payType,
            ticketName = item.name,
            price = item.price,
        )
    }
}
