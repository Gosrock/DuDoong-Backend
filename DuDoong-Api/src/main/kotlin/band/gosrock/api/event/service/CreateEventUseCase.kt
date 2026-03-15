package band.gosrock.api.event.service

import band.gosrock.api.common.UserUtils
import band.gosrock.api.event.model.dto.request.CreateEventRequest
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.domain.domains.host.service.HostService
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateEventUseCase(
    private val userUtils: UserUtils,
    private val hostService: HostService,
    private val eventService: EventService,
    private val eventMapper: EventMapper
) {
    @Transactional
    fun execute(createEventRequest: CreateEventRequest): EventResponse {
        val userId = userUtils.getCurrentUserId()
        // 슈퍼 호스트 이상만 공연 생성 가능
        hostService.validateManagerHostUser(createEventRequest.hostId!!, userId)
        val event = eventMapper.toEntity(createEventRequest)
        return EventResponse.of(eventService.createEvent(event))
    }
}
