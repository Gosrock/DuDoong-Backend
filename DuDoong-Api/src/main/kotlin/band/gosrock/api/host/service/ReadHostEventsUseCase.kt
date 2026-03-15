package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.host.model.dto.response.HostEventProfileResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class ReadHostEventsUseCase(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
) {
    @HostRolesAllowed(role = GUEST, findHostFrom = HOST_ID)
    fun execute(hostId: Long, pageable: Pageable): PageResponse<HostEventProfileResponse> {
        val host = hostAdaptor.findById(hostId)
        return PageResponse.of(
            eventAdaptor
                .findAllByHostId(hostId, pageable)
                .map { event -> HostEventProfileResponse.of(host, event) }
        )
    }
}
