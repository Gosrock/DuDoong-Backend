package band.gosrock.domain.domains.issuedTicket.repository

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface IssuedTicketCustomRepository {
    fun searchToPage(condition: FindEventIssuedTicketsCondition, pageable: Pageable): Page<IssuedTicket>
    fun find(issuedTicketId: Long): Optional<IssuedTicket>
    fun countPaidTicket(userId: Long, issuedTicketId: Long): Long
    fun countIssuedTicketByItemId(ticketItemId: Long): Long
}
