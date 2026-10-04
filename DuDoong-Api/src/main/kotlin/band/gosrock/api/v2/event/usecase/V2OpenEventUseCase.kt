package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.response.V2EventStatusResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2OpenEventUseCase(
    private val eventAdaptor: EventAdaptor,
    private val v2EventDomainService: V2EventDomainService,
) {
    private val log = LoggerFactory.getLogger(V2OpenEventUseCase::class.java)

    /** 체크리스트 미충족 400 (Event_400_7). hasTicket=false 면 티켓 면제는 이 v2 경로에서만 적용된다 */
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): V2EventStatusResponse {
        log.info("[V2OpenEventUseCase][execute] 공연 등록 userId={} eventId={}", userId, eventId)
        val event = v2EventDomainService.openEvent(eventAdaptor.findById(eventId))
        return V2EventStatusResponse.of(event, LocalDateTime.now())
    }
}
