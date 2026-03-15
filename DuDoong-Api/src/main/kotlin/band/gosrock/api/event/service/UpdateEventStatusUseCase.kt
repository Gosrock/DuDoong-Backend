package band.gosrock.api.event.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.event.model.dto.request.UpdateEventStatusRequest
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateEventStatusUseCase(
    private val eventService: EventService,
    private val eventAdaptor: EventAdaptor
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(eventId: Long, updateEventStatusRequest: UpdateEventStatusRequest): EventResponse {
        val event = eventAdaptor.findById(eventId)
        val status = updateEventStatusRequest.status!!
        return EventResponse.of(eventService.updateEventStatus(event, status))
    }
}
