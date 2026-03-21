package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateEventRequest
import band.gosrock.admin.model.dto.response.AdminEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateEventUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventRepository: EventRepository,
    private val hostAdaptor: HostAdaptor,
) {

    @Transactional
    fun execute(eventId: Long, request: AdminUpdateEventRequest): AdminEventResponse {
        val event = eventAdaptor.findById(eventId)
        // 어드민은 OPEN 상태에서도 수정 가능하도록 직접 필드 수정
        event.adminUpdate(
            name = request.name,
            startAt = request.startAt,
            runTime = request.runTime?.toLong(),
            content = request.content,
            placeName = request.placeName,
            placeAddress = request.placeAddress,
        )
        eventRepository.save(event)

        val hostName = event.hostId?.let {
            runCatching { hostAdaptor.findById(it).profile?.name }.getOrNull()
        }
        return AdminEventResponse.of(event, hostName)
    }
}
