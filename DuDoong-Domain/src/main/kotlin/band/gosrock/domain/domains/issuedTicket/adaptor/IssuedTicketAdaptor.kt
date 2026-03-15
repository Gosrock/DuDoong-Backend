package band.gosrock.domain.domains.issuedTicket.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketUserNotMatchedException
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Adaptor
class IssuedTicketAdaptor(
    private val issuedTicketRepository: IssuedTicketRepository,
) {
    fun save(issuedTicket: IssuedTicket): IssuedTicket =
        issuedTicketRepository.save(issuedTicket)

    fun saveAll(issuedTickets: List<IssuedTicket>) {
        issuedTicketRepository.saveAll(issuedTickets)
    }

    fun findForUser(currentUserId: Long, uuid: String): IssuedTicket {
        val issuedTicket = issuedTicketRepository.findByUuid(uuid)
            .orElseThrow { IssuedTicketNotFoundException.EXCEPTION }
        if (issuedTicket.userInfo?.userId != currentUserId) {
            throw IssuedTicketUserNotMatchedException.EXCEPTION
        }
        return issuedTicket
    }

    fun queryIssuedTicket(issuedTicketId: Long): IssuedTicket =
        issuedTicketRepository.find(issuedTicketId)
            .orElseThrow { IssuedTicketNotFoundException.EXCEPTION }

    fun existsByEventId(eventId: Long): Boolean =
        issuedTicketRepository.existsByEventId(eventId)

    fun searchIssuedTicket(page: Pageable, condition: FindEventIssuedTicketsCondition): Page<IssuedTicket> =
        issuedTicketRepository.searchToPage(condition, page)

    fun cancel(issuedTicket: IssuedTicket) {
        issuedTicket.cancel()
    }

    fun findAllByOrderUuid(orderUuid: String): List<IssuedTicket> =
        issuedTicketRepository.findAllByOrderUuid(orderUuid)

    fun findOrderLineIssuedTickets(orderLineId: Long): IssuedTickets =
        IssuedTickets.from(issuedTicketRepository.findAllByOrderLineId(orderLineId))

    fun findOrderIssuedTickets(orderUuid: String): IssuedTickets =
        IssuedTickets.from(issuedTicketRepository.findAllByOrderUuid(orderUuid))

    fun countPaidTicket(userId: Long, itemId: Long): Long =
        issuedTicketRepository.countPaidTicket(userId, itemId)

    fun countIssuedTicketByItemId(itemId: Long): Long =
        issuedTicketRepository.countIssuedTicketByItemId(itemId)

    fun queryByIssuedTicketNo(issuedTicketNo: String): IssuedTicket =
        issuedTicketRepository.findByIssuedTicketNo(issuedTicketNo)
            .orElseThrow { IssuedTicketNotFoundException.EXCEPTION }

    fun queryByIssuedTicketUuid(uuid: String): IssuedTicket =
        issuedTicketRepository.findByUuid(uuid)
            .orElseThrow { IssuedTicketNotFoundException.EXCEPTION }
}
