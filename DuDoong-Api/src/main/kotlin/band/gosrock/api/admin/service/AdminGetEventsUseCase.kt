package band.gosrock.api.admin.service

import band.gosrock.api.admin.model.dto.response.AdminEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetEventsUseCase(
    private val eventRepository: EventRepository,
    private val hostAdaptor: HostAdaptor,
) {

    fun execute(keyword: String?, status: String?, pageable: Pageable): Page<AdminEventResponse> {
        return eventRepository.findAllForAdmin(keyword, status, pageable)
            .map { event ->
                val hostName = event.hostId?.let {
                    runCatching { hostAdaptor.findById(it).profile?.name }.getOrNull()
                }
                AdminEventResponse.of(event, hostName)
            }
    }
}
