package band.gosrock.api.order.model.mapper

import band.gosrock.api.order.model.dto.response.CreateOrderResponse
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement
import band.gosrock.api.order.model.dto.response.OrderBriefElement
import band.gosrock.api.order.model.dto.response.OrderLineTicketResponse
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.dto.response.OrderTicketResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.Option
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Slice
import org.springframework.transaction.annotation.Transactional

@Mapper
class OrderMapper(
    private val orderAdaptor: OrderAdaptor,
    private val userAdaptor: UserAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val optionAdaptor: OptionAdaptor,
    private val eventAdaptor: EventAdaptor,
) {
    @Transactional(readOnly = true)
    fun toOrderResponse(orderUuid: String): OrderResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val event = getEvent(order)
        val orderLineTicketResponses = getOrderLineTicketResponses(order)
        return OrderResponse.of(order, event, orderLineTicketResponses)
    }

    @Transactional(readOnly = true)
    fun toOrderResponse(order: Order): OrderResponse {
        val event = getEvent(order)
        val orderLineTicketResponses = getOrderLineTicketResponses(order)
        return OrderResponse.of(order, event, orderLineTicketResponses)
    }

    @Transactional(readOnly = true)
    fun toCreateOrderResponse(orderUuid: String): CreateOrderResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val user = userAdaptor.queryUser(order.userId)
        val item = ticketItemAdaptor.queryTicketItem(order.itemId)
        return CreateOrderResponse.from(order, item, user.profile!!)
    }

    private fun getOrderLineTicketResponses(order: Order): List<OrderLineTicketResponse> {
        val user = userAdaptor.queryUser(order.userId)
        return order.orderLineItems.map { orderLineItem ->
            OrderLineTicketResponse.of(
                order,
                orderLineItem,
                getOptionAnswerVos(orderLineItem),
                user.profile!!.name!!,
                getTicketNoName(orderLineItem.id!!),
            )
        }
    }

    private fun getOptionAnswerVos(orderLineItem: OrderLineItem): List<OptionAnswerVo> {
        // TODO: options 일급 컬렉션으로 리팩터링
        val options = optionAdaptor.findAllByIds(orderLineItem.getAnswerOptionIds())
        return orderLineItem.orderOptionAnswers.map { orderOptionAnswer ->
            orderOptionAnswer.getOptionAnswerVo(getOption(options, orderOptionAnswer))
        }
    }

    private fun getOption(options: List<Option>, orderOptionAnswer: OrderOptionAnswer): Option =
        options.first { it.id == orderOptionAnswer.optionId }

    private fun getTicketNoName(orderLineItemId: Long): String =
        issuedTicketAdaptor.findOrderLineIssuedTickets(orderLineItemId).getTicketNoName()

    fun toOrderBriefElement(order: Order): OrderBriefElement {
        val orderIssuedTickets = issuedTicketAdaptor.findOrderIssuedTickets(order.uuid!!)
        return OrderBriefElement.of(order, getEvent(order), orderIssuedTickets)
    }

    fun toOrderBriefsResponse(ordersWithPagination: Slice<Order>): Slice<OrderBriefElement> =
        ordersWithPagination.map { toOrderBriefElement(it) }

    fun toOrderAdminTableElement(eventId: Long, orders: Page<Order>): Page<OrderAdminTableElement> {
        val userIds = orders.map { it.userId!! }.distinct().toList()
        val users = userAdaptor.findUserByIdIn(userIds)
        val event = eventAdaptor.findById(eventId)
        return orders.map { order ->
            val user = users.first { it.id == order.userId }
            OrderAdminTableElement.of(order, event, user)
        }
    }

    private fun getEvent(order: Order): Event = eventAdaptor.findById(order.getItemGroupId())

    fun toOrderTicketResponse(order: Order): OrderTicketResponse {
        val orderIssuedTickets = issuedTicketAdaptor.findOrderIssuedTickets(order.uuid!!)
        val event = getEvent(order)
        val issuedTicketInfoVos: List<IssuedTicketInfoVo> = orderIssuedTickets.getIssuedTicketInfoVos()
        return OrderTicketResponse.of(order, event, issuedTicketInfoVos)
    }
}
