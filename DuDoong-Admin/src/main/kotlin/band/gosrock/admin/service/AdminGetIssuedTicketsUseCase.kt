package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminIssuedTicketResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetIssuedTicketsUseCase(
    private val issuedTicketRepository: IssuedTicketRepository,
) {

    fun execute(eventId: Long, pageable: Pageable): Page<AdminIssuedTicketResponse> {
        return issuedTicketRepository.findAllByEventId(eventId, pageable)
            .map { AdminIssuedTicketResponse.from(it) }
    }
}
