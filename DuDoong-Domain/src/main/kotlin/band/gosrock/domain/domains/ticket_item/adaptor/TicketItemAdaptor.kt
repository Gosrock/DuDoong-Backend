package band.gosrock.domain.domains.ticket_item.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotFoundException
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository

@Adaptor
class TicketItemAdaptor(
    private val ticketItemRepository: TicketItemRepository
) {

    fun queryTicketItem(ticketItemId: Long?): TicketItem {
        return ticketItemRepository
            .findByIdAndTicketItemStatus(ticketItemId ?: throw TicketItemNotFoundException.EXCEPTION, TicketItemStatus.VALID)
            .orElseThrow { TicketItemNotFoundException.EXCEPTION }
    }

    fun findAllByEventId(eventId: Long?): List<TicketItem> {
        return ticketItemRepository.findAllByEventIdAndTicketItemStatus(
            eventId ?: return emptyList(), TicketItemStatus.VALID
        )
    }

    fun existsByEventId(eventId: Long): Boolean {
        return ticketItemRepository.existsByEventId(eventId)
    }

    /** 유효(삭제 안 된) 티켓 존재 여부 (v2 체크리스트) */
    fun existsValidByEventId(eventId: Long): Boolean =
        ticketItemRepository.existsByEventIdAndTicketItemStatus(eventId, TicketItemStatus.VALID)

    fun save(ticketItem: TicketItem): TicketItem {
        return ticketItemRepository.save(ticketItem)
    }
}
