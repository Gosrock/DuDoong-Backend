package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
class IssuedTicketItemInfoVo() {

    var ticketItemId: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    var ticketType: TicketType? = null
        protected set

    @Enumerated(EnumType.STRING)
    var payType: TicketPayType? = null
        protected set

    var ticketName: String? = null
        protected set

    var price: Money? = null
        protected set

    constructor(
        ticketItemId: Long?,
        ticketType: TicketType?,
        payType: TicketPayType?,
        ticketName: String?,
        price: Money?,
    ) : this() {
        this.ticketItemId = ticketItemId
        this.ticketType = ticketType
        this.payType = payType
        this.ticketName = ticketName
        this.price = price
    }

    companion object {
        @JvmStatic
        fun from(item: TicketItem): IssuedTicketItemInfoVo = IssuedTicketItemInfoVo(
            ticketItemId = item.id,
            ticketType = item.type,
            payType = item.payType,
            ticketName = item.name,
            price = item.price,
        )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var ticketItemId: Long? = null
        private var ticketType: TicketType? = null
        private var payType: TicketPayType? = null
        private var ticketName: String? = null
        private var price: Money? = null

        fun ticketItemId(ticketItemId: Long?) = apply { this.ticketItemId = ticketItemId }
        fun ticketType(ticketType: TicketType?) = apply { this.ticketType = ticketType }
        fun payType(payType: TicketPayType?) = apply { this.payType = payType }
        fun ticketName(ticketName: String?) = apply { this.ticketName = ticketName }
        fun price(price: Money?) = apply { this.price = price }

        fun build(): IssuedTicketItemInfoVo = IssuedTicketItemInfoVo(ticketItemId, ticketType, payType, ticketName, price)
    }
}
