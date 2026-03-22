package band.gosrock.api.event.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateEventBasicUseCase(
    private val eventService: EventService,
    private val eventAdaptor: EventAdaptor,
    private val eventMapper: EventMapper
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long, updateEventBasicRequest: UpdateEventBasicRequest): EventResponse {
        val event = eventAdaptor.findById(eventId)
        val eventBasic = eventMapper.toEventBasic(updateEventBasicRequest)
        val eventPlace = eventMapper.toEventPlace(updateEventBasicRequest)
        return EventResponse.of(eventService.updateEventBasic(event, eventBasic, eventPlace))
    }
}
