package band.gosrock.api.event.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateEventDetailUseCase(
    private val eventService: EventService,
    private val eventAdaptor: EventAdaptor,
    private val eventMapper: EventMapper
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(eventId: Long, updateEventDetailRequest: UpdateEventDetailRequest): EventResponse {
        val event = eventAdaptor.findById(eventId)
        return EventResponse.of(
            eventService.updateEventDetail(event, eventMapper.toEventDetail(updateEventDetailRequest))
        )
    }
}
