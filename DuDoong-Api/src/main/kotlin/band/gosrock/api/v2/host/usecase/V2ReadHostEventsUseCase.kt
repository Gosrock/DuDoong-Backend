package band.gosrock.api.v2.host.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.host.dto.response.V2HostEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadHostEventsUseCase(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
) {
    /** 공개 API. 비멤버는 공개 공연만, 활성 멤버는 준비중 포함. 삭제된 공연은 항상 제외. 최근 생성 순 */
    @Transactional(readOnly = true)
    fun execute(userId: Long, hostId: Long, page: Int, size: Int): V2PageResponse<V2HostEventResponse> {
        val host = hostAdaptor.findById(hostId)
        val statuses = if (host.isActiveHostUserId(userId)) MEMBER_STATUSES else PUBLIC_STATUSES
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val now = LocalDateTime.now()
        return V2PageResponse.of(
            eventAdaptor.findAllByHostIdAndStatusIn(hostId, statuses, pageable).map { V2HostEventResponse.of(it, now) }
        )
    }

    companion object {
        private val PUBLIC_STATUSES = listOf(EventStatus.OPEN, EventStatus.CALCULATING, EventStatus.CLOSED)
        private val MEMBER_STATUSES = PUBLIC_STATUSES + EventStatus.PREPARING
    }
}
