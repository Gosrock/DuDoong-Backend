package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminTicketItemResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetTicketItemsUseCase(
    private val ticketItemAdaptor: TicketItemAdaptor,
) {

    fun execute(eventId: Long): List<AdminTicketItemResponse> {
        return ticketItemAdaptor.findAllByEventId(eventId)
            .map { AdminTicketItemResponse.from(it) }
    }
}
