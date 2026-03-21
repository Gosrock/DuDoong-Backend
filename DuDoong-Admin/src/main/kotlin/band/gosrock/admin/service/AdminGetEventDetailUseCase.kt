package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import band.gosrock.domain.domains.order.repository.OrderRepository
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetEventDetailUseCase(
    private val eventRepository: EventRepository,
    private val hostAdaptor: HostAdaptor,
    private val ticketItemRepository: TicketItemRepository,
    private val issuedTicketRepository: IssuedTicketRepository,
    private val orderRepository: OrderRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, eventId: Long): AdminEventResponse {
        adminAuthValidator.validateManagerOrAbove(userId)
        val event = eventRepository.findByIdForAdmin(eventId)
            ?: throw EventNotFoundException.EXCEPTION

        val hostName = event.hostId?.let {
            runCatching { hostAdaptor.findById(it).profile?.name }.getOrNull()
        }
        val ticketItemCount = ticketItemRepository.countByEventId(eventId).toInt()
        val issuedTicketCount = issuedTicketRepository.countByEventId(eventId).toInt()
        val totalOrderCount = orderRepository.countByEventId(eventId).toInt()

        return AdminEventResponse.ofDetail(
            event = event,
            hostName = hostName,
            ticketItemCount = ticketItemCount,
            issuedTicketCount = issuedTicketCount,
            totalOrderCount = totalOrderCount,
        )
    }
}
