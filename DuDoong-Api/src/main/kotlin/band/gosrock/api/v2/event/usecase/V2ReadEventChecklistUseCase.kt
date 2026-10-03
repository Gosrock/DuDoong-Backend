package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.response.V2EventChecklistResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.EventService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadEventChecklistUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventService: EventService,
) {
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): V2EventChecklistResponse =
        V2EventChecklistResponse.from(eventService.getChecklistV2(eventAdaptor.findById(eventId)))
}
