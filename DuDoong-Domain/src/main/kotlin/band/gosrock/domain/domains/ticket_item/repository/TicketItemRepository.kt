package band.gosrock.domain.domains.ticket_item.repository

import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TicketItemRepository : JpaRepository<TicketItem, Long> {

    fun findAllByEventIdAndTicketItemStatus(eventId: Long, status: TicketItemStatus): List<TicketItem>

    fun existsByEventId(eventId: Long): Boolean

    fun findByIdAndTicketItemStatus(ticketItemId: Long, status: TicketItemStatus): Optional<TicketItem>
}
