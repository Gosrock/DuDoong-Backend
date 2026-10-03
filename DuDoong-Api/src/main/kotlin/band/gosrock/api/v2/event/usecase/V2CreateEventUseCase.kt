package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.request.V2CreateEventRequest
import band.gosrock.api.v2.event.dto.response.V2CreateEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2CreateEventUseCase(
    private val eventService: EventService,
) {
    /** 간편 생성. 상태 PREPARING, runTime 은 endAt - startAt(분) 으로 함께 저장 (v1 호환) */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: V2CreateEventRequest): V2CreateEventResponse {
        val event = Event.createV2(
            hostId = hostId,
            name = request.name!!.trim(),
            startAt = request.startAt!!,
            endAt = request.endAt!!,
            hasTicket = request.hasTicket!!,
        )
        return V2CreateEventResponse(eventId = eventService.createEvent(event).id!!)
    }
}
