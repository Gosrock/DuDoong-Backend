package band.gosrock.domain.domains.order.domain.validator

import band.gosrock.common.annotation.Validator
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.exception.ApproveWaitingOrderPurchaseLimitException
import band.gosrock.domain.domains.order.exception.CanNotApproveDeletedUserOrderException
import band.gosrock.domain.domains.order.exception.CanNotCancelOrderException
import band.gosrock.domain.domains.order.exception.CanNotRefundOrderException
import band.gosrock.domain.domains.order.exception.InvalidOrderException
import band.gosrock.domain.domains.order.exception.NotApprovalOrderException
import band.gosrock.domain.domains.order.exception.NotFreeOrderException
import band.gosrock.domain.domains.order.exception.NotOwnerOrderException
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException
import band.gosrock.domain.domains.order.exception.NotPendingOrderException
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException
import band.gosrock.domain.domains.order.exception.OrdeItemNotOneTypeException
import band.gosrock.domain.domains.order.exception.OrderItemOptionChangedException
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.Option
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import java.util.Objects

@Validator
class OrderValidator(
    private val eventAdaptor: EventAdaptor,
    private val itemAdaptor: TicketItemAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val optionAdaptor: OptionAdaptor,
    private val userAdaptor: UserAdaptor,
    private val orderAdaptor: OrderAdaptor,
) {
    fun validCanCreate(order: Order) {
        val item = getItem(order)
        val event = getEvent(order)
        validEventIsOpen(event)
        validTicketingTime(event)
        validItemStockEnough(order, item)
        validItemKindIsOneType(order)
        validItemPurchaseLimit(order, item)
        validOptionNotChange(order, item)
    }

    fun validOptionNotChange(order: Order, item: TicketItem) {
        val orderLineItems = order.orderLineItems
        val itemsOptionGroupIds = item.getOptionGroupIds()
        orderLineItems.forEach { orderLineItem ->
            if (!Objects.equals(getAnswerOptionGroupIds(orderLineItem), itemsOptionGroupIds)) {
                throw OrderItemOptionChangedException.EXCEPTION
            }
        }
    }

    fun validCanApproveOrder(order: Order) {
        validMethodIsCanApprove(order)
        validStatusCanApprove(order.orderStatus)
        validCanDone(order)
        validUserNotDeleted(order)
    }

    fun validUserNotDeleted(order: Order) {
        val user = userAdaptor.queryUser(order.userId!!)
        if (user.isDeletedUser()) {
            throw CanNotApproveDeletedUserOrderException.EXCEPTION
        }
    }

    fun validCanConfirmPayment(order: Order) {
        validMethodIsPaymentOrder(order)
        validStatusCanPaymentConfirm(order.orderStatus)
        validCanDone(order)
    }

    fun validCanFreeConfirm(order: Order) {
        validAmountIsFree(order)
        validStatusCanPaymentConfirm(order.orderStatus)
        validCanDone(order)
    }

    fun validCanCancel(order: Order) {
        validAvailableRefundDate(order)
        validStatusCanCancel(order.orderStatus)
        validCanWithDraw(order)
    }

    fun validCanRefuse(order: Order) {
        validAvailableRefundDate(order)
        validStatusCanRefuse(order.orderStatus)
        validCanWithDraw(order)
    }

    fun validCanRefund(order: Order) {
        validAvailableRefundDate(order)
        validStatusCanRefund(order.orderStatus)
        validCanWithDraw(order)
    }

    fun validCanDone(order: Order) {
        val item = getItem(order)
        val event = getEvent(order)
        validEventIsOpen(event)
        validTicketingTime(event)
        validItemStockEnough(order, item)
        validItemPurchaseLimit(order, item)
        validOptionNotChange(order, item)
    }

    fun validCanWithDraw(order: Order) {
        val event = getEvent(order)
        validEventIsOpen(event)
        validTicketingTime(event)
    }

    fun validItemPurchaseLimit(order: Order, item: TicketItem) {
        val paidTicketCount = issuedTicketAdaptor.countPaidTicket(order.userId!!, item.id!!)
        val totalIssuedCount = paidTicketCount + order.getTotalQuantity()
        item.validPurchaseLimit(totalIssuedCount)
    }

    fun validApproveStatePurchaseLimit(order: Order) {
        val item = getItem(order)
        val userId = order.userId!!
        val paidTicketCount = issuedTicketAdaptor.countPaidTicket(userId, item.id!!)
        val approveWaitingOrders = orderAdaptor.findByEventIdAndOrderStatusAndUserId(
            order.eventId!!, userId, OrderStatus.PENDING_APPROVE
        )
        val approveWaitingTicketCount = approveWaitingOrders
            .filter { Objects.equals(item.id, it.itemId) }
            .sumOf { it.getTotalQuantity() }
        val totalIssuedCount = paidTicketCount + approveWaitingTicketCount + order.getTotalQuantity()
        if (item.isPurchaseLimitExceed(totalIssuedCount)) {
            throw ApproveWaitingOrderPurchaseLimitException.EXCEPTION
        }
    }

    fun validApproveOrderCreateTotalStock(order: Order) {
        val item = getItem(order)
        val approveWaitingOrders = orderAdaptor.findByEventIdAndOrderStatus(
            order.eventId!!, OrderStatus.PENDING_APPROVE
        )
        val approveWaitingTicketCount = approveWaitingOrders.sumOf { it.getTotalQuantity() }
        val expectedApproveWaitQuantity = approveWaitingTicketCount + order.getTotalQuantity()
        item.validEnoughQuantity(expectedApproveWaitQuantity)
    }

    fun validEventIsOpen(event: Event) {
        event.validateNotOpenStatus()
    }

    fun validItemKindIsOneType(order: Order) {
        val itemIds = order.getDistinctItemIds()
        if (itemIds.size != 1) {
            throw OrdeItemNotOneTypeException.EXCEPTION
        }
    }

    fun validTicketingTime(event: Event) {
        event.validateTicketingTime()
    }

    fun validItemStockEnough(order: Order, item: TicketItem) {
        item.validEnoughQuantity(order.getTotalQuantity())
    }

    fun validMethodIsCanApprove(order: Order) {
        if (isMethodPayment(order)) {
            throw NotApprovalOrderException.EXCEPTION
        }
    }

    fun validOwner(order: Order, currentUserId: Long) {
        if (order.userId != currentUserId) {
            throw NotOwnerOrderException.EXCEPTION
        }
    }

    fun validAmountIsSameAsRequest(order: Order, requestAmount: Money) {
        if (order.getTotalPaymentPrice() != requestAmount) {
            throw InvalidOrderException.EXCEPTION
        }
    }

    fun validMethodIsPaymentOrder(order: Order) {
        if (!isMethodPayment(order)) {
            throw NotPaymentOrderException.EXCEPTION
        }
    }

    fun validAvailableRefundDate(order: Order) {
        if (!isRefundDateNotPassed(order)) {
            throw NotRefundAvailableDateOrderException.EXCEPTION
        }
    }

    fun validAmountIsFree(order: Order) {
        if (order.isNeedPaid()) {
            throw NotFreeOrderException.EXCEPTION
        }
    }

    fun isRefundDateNotPassed(order: Order): Boolean {
        val events = eventAdaptor.findAllByIds(getEventIds(order))
        return events.map { it.isRefundDateNotPassed() }.reduce { a, b -> a && b }
    }

    private fun getEventIds(order: Order): List<Long> =
        order.orderLineItems.map { it.orderItem!!.itemGroupId!! }

    private fun isMethodPayment(order: Order): Boolean = order.orderMethod!!.isPayment()

    fun isStatusCanWithDraw(orderStatus: OrderStatus): Boolean =
        orderStatus == OrderStatus.CONFIRM || orderStatus == OrderStatus.APPROVED

    fun isStatusCanRefuse(orderStatus: OrderStatus): Boolean =
        orderStatus == OrderStatus.PENDING_APPROVE

    fun validStatusCanCancel(orderStatus: OrderStatus) {
        if (!isStatusCanWithDraw(orderStatus)) throw CanNotCancelOrderException.EXCEPTION
    }

    fun validStatusCanRefund(orderStatus: OrderStatus) {
        if (!isStatusCanWithDraw(orderStatus)) throw CanNotRefundOrderException.EXCEPTION
    }

    fun validStatusCanRefuse(orderStatus: OrderStatus) {
        if (!isStatusCanRefuse(orderStatus)) throw CanNotCancelOrderException.EXCEPTION
    }

    fun validStatusCanPaymentConfirm(orderStatus: OrderStatus) {
        if (orderStatus != OrderStatus.PENDING_PAYMENT) throw NotPendingOrderException.EXCEPTION
    }

    fun validStatusCanApprove(orderStatus: OrderStatus) {
        if (orderStatus != OrderStatus.PENDING_APPROVE) throw NotPendingOrderException.EXCEPTION
    }

    private fun getEvent(order: Order): Event = eventAdaptor.findById(order.getItemGroupId())

    private fun getItem(order: Order): TicketItem = itemAdaptor.queryTicketItem(order.itemId)

    private fun getAnswerOptionGroupIds(orderLineItem: OrderLineItem): List<Long> {
        val answerOptions = getOptionsFrom(orderLineItem)
        return answerOptions.map { it.getOptionGroupId()!! }.sorted()
    }

    private fun getOptionsFrom(orderLineItem: OrderLineItem): List<Option> =
        optionAdaptor.findAllByIds(orderLineItem.getAnswerOptionIds())
}
