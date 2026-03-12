package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import javax.persistence.Embeddable

@Embeddable
class OrderItemVo() {

    var name: String? = null
        protected set

    var price: Money? = null
        protected set

    var itemGroupId: Long? = null
        protected set

    var itemId: Long? = null
        protected set

    companion object {
        @JvmStatic
        fun from(ticketItem: TicketItem): OrderItemVo = OrderItemVo().apply {
            itemGroupId = ticketItem.eventId
            itemId = ticketItem.id
            price = ticketItem.price
            name = ticketItem.name
        }
    }
}
