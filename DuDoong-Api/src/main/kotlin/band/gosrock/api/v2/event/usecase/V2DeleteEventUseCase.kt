package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.response.V2EventStatusResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2DeleteEventUseCase(
    private val eventAdaptor: EventAdaptor,
    private val v2EventDomainService: V2EventDomainService,
) {
    /** 준비중 공연만 소프트 삭제 (Event_400_19). 발급 티켓이 있으면 불가 (v1 규칙) */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): V2EventStatusResponse =
        V2EventStatusResponse.of(v2EventDomainService.deleteEventSoft(eventAdaptor.findById(eventId)), LocalDateTime.now())
}
