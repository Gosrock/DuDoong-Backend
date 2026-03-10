package band.gosrock.domain.domains.ticket_item.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemException
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class TicketItemService(
    private val ticketItemAdaptor: TicketItemAdaptor
) {

    @Transactional
    fun createTicketItem(ticketItem: TicketItem, isPartner: Boolean): TicketItem {
        ticketItem.validateTicketPayType(isPartner)
        return ticketItemAdaptor.save(ticketItem)
    }

    fun validateExistenceByEventId(eventId: Long) {
        if (!ticketItemAdaptor.existsByEventId(eventId)) {
            throw InvalidTicketItemException.EXCEPTION
        }
    }

    @RedissonLock(LockName = "티켓관리", identifier = "ticketItemId")
    fun softDeleteTicketItem(eventId: Long, ticketItemId: Long) {
        val ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId)
        // 해당 eventId에 속해 있는 티켓 아이템이 맞는지 확인
        ticketItem.validateEventId(eventId)
        ticketItem.softDeleteTicketItem()
        ticketItemAdaptor.save(ticketItem)
    }
}
