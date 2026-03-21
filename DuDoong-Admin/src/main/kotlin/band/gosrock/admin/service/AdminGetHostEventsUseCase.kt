package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetHostEventsUseCase(
    private val eventRepository: EventRepository,
    private val hostAdaptor: HostAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, hostId: Long, pageable: Pageable): Page<AdminEventResponse> {
        adminAuthValidator.validateManagerOrAbove(userId)
        val host = hostAdaptor.findById(hostId)
        val hostName = host.profile?.name
        return eventRepository.findAllByHostId(hostId, pageable)
            .map { AdminEventResponse.of(it, hostName) }
    }
}
