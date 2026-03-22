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
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, eventId: Long, pageable: Pageable): Page<AdminIssuedTicketResponse> {
        adminAuthValidator.validateAdminOrAbove(userId)
        return issuedTicketRepository.findAllByEventId(eventId, pageable)
            .map { AdminIssuedTicketResponse.from(it) }
    }

    fun executeAll(userId: Long, eventId: Long): List<AdminIssuedTicketResponse> {
        adminAuthValidator.validateAdminOrAbove(userId)
        return issuedTicketRepository.findAllByEventId(eventId)
            .map { AdminIssuedTicketResponse.from(it) }
    }
}
