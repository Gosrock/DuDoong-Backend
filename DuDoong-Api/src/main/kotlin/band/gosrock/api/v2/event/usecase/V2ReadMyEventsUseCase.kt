package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.event.dto.response.V2MyEventResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadMyEventsUseCase(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
) {
    /** 내가 활성 멤버인 모든 호스트(일반 포함)의 공연. 삭제 제외, keyword 는 공연명 부분일치, 최신 생성 순 */
    @Transactional(readOnly = true)
    fun execute(userId: Long, keyword: String?, page: Int, size: Int): V2PageResponse<V2MyEventResponse> {
        val hostNames = hostAdaptor.querySliceHostsByActiveUserId(userId).associate { it.id!! to it.profile?.name }
        val events = eventAdaptor.queryPageEventsByHostIdInAndKeyword(
            hostNames.keys.toList(),
            keyword?.trim(),
            PageRequest.of(page, size),
        )
        val now = LocalDateTime.now()
        return V2PageResponse.of(events.map { V2MyEventResponse.of(it, hostNames[it.hostId], now) })
    }
}
