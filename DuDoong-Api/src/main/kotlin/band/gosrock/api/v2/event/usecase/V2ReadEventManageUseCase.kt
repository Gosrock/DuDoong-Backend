package band.gosrock.api.v2.event.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.event.dto.response.V2EventManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadEventManageUseCase(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val tagAdaptor: TagAdaptor,
    private val v2EventDomainService: V2EventDomainService,
) {
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): V2EventManageResponse = toResponse(userId, eventAdaptor.findById(eventId))

    /** 수정 API 응답에도 쓴다 */
    fun toResponse(userId: Long, event: Event): V2EventManageResponse {
        val now = LocalDateTime.now()
        val tags = tagAdaptor.findAllByIdIn(event.getTagIds())
            .sortedWith(compareBy({ it.category.ordinal }, { it.sortOrder }, { it.id }))
        return V2EventManageResponse.of(
            event = event,
            host = hostAdaptor.findById(event.hostId!!),
            userId = userId,
            tags = tags,
            checklist = v2EventDomainService.checklist(event, now),
            now = now,
        )
    }
}
