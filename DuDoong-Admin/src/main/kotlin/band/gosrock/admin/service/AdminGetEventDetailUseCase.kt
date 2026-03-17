package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetEventDetailUseCase(
    private val eventRepository: EventRepository,
    private val hostAdaptor: HostAdaptor,
) {

    fun execute(eventId: Long): AdminEventResponse {
        val event = eventRepository.findByIdForAdmin(eventId)
            ?: throw EventNotFoundException.EXCEPTION

        val hostName = event.hostId?.let {
            runCatching { hostAdaptor.findById(it).profile?.name }.getOrNull()
        }
        return AdminEventResponse.of(event, hostName)
    }
}
