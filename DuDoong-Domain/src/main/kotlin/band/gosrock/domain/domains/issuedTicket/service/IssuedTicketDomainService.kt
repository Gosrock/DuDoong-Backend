package band.gosrock.domain.domains.issuedTicket.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class IssuedTicketDomainService(
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val userAdaptor: UserAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val issuedTicketValidator: IssuedTicketValidator,
) {

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    fun withdrawIssuedTicket(itemId: Long, issuedTickets: List<IssuedTicket>) {
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        issuedTickets.forEach { issuedTicket ->
            ticketItem.increaseQuantity(1L)
            issuedTicket.cancel()
        }
    }

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    fun doneOrderEventAfterRollBackWithdrawIssuedTickets(itemId: Long, orderUuid: String) {
        val failIssuedTickets = issuedTicketAdaptor.findAllByOrderUuid(orderUuid)
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        failIssuedTickets.forEach { issuedTicket ->
            ticketItem.increaseQuantity(1L)
            issuedTicket.cancel()
        }
    }

    fun processingEntranceIssuedTicket(eventId: Long, uuid: String): IssuedTicketInfoVo {
        val issuedTicket = issuedTicketAdaptor.queryByIssuedTicketUuid(uuid)
        issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, eventId)
        issuedTicket.entrance()
        return issuedTicket.toIssuedTicketInfoVo()
    }

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    fun createIssuedTicket(itemId: Long, orderUuid: String, userId: Long) {
        val ticketItem = ticketItemAdaptor.queryTicketItem(itemId)
        val user = userAdaptor.queryUser(userId)
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val orderLineItems = order.orderLineItems
        val issuedTickets = orderLineItems.flatMap { orderLineItem ->
            ticketItem.reduceQuantity(orderLineItem.quantity)
            IssuedTicket.orderLineToIssuedTicket(ticketItem, user, order, order.eventId, orderLineItem)
        }
        issuedTicketAdaptor.saveAll(issuedTickets)
    }
}
