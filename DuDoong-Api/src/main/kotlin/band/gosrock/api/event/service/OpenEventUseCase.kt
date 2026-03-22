package band.gosrock.api.event.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.EventService
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

@UseCase
class OpenEventUseCase(
    private val eventService: EventService,
    private val eventAdaptor: EventAdaptor
) {
    private val log = LoggerFactory.getLogger(OpenEventUseCase::class.java)

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): EventResponse {
        log.info("[OpenEventUseCase][execute] 이벤트 오픈 userId={} eventId={}", userId, eventId)
        val event = eventAdaptor.findById(eventId)
        return EventResponse.of(eventService.openEvent(event))
    }
}
