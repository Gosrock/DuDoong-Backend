package band.gosrock.api.event.service

import band.gosrock.api.common.aop.hostRole.SuperAdminBypass
import band.gosrock.api.event.model.dto.request.CreateEventRequest
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.domain.domains.host.service.HostService
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateEventUseCase(
    private val hostService: HostService,
    private val eventService: EventService,
    private val eventMapper: EventMapper,
    private val superAdminBypass: SuperAdminBypass,
) {
    private val log = LoggerFactory.getLogger(CreateEventUseCase::class.java)

    @Transactional
    fun execute(userId: Long, createEventRequest: CreateEventRequest): EventResponse {
        log.info("[CreateEventUseCase][execute] 이벤트 생성 userId={} hostId={}", userId, createEventRequest.hostId)
        // 슈퍼 호스트 이상만 공연 생성 가능. SUPER_ADMIN 은 v2 공연 생성(@HostRolesAllowed)과 같이 예외 (#763)
        val hostId = createEventRequest.hostId!!
        if (!superAdminBypass.bypass(userId, "CreateEventUseCase.execute", "HOST", hostId)) {
            hostService.validateManagerHostUser(hostId, userId)
        }
        val event = eventMapper.toEntity(createEventRequest)
        return EventResponse.of(eventService.createEvent(event))
    }
}
