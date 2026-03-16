package band.gosrock.api.event.service

import band.gosrock.api.event.model.dto.response.EventDetailResponse
import band.gosrock.api.event.model.mapper.EventMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.exception.EventNotOpenException
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class ReadEventDetailUseCase(
    private val eventAdaptor: EventAdaptor,
    private val eventMapper: EventMapper,
    private val hostAdaptor: HostAdaptor
) {
    fun execute(userId: Long, eventId: Long): EventDetailResponse {
        val event = eventAdaptor.findById(eventId)
        val host = hostAdaptor.findById(event.hostId!!)
        // 호스트 유저가 아닐 경우 준비 상태일 때 조회할 수 없음
        if (event.isPreparing() && !host.isActiveHostUserId(userId)) {
            throw EventNotOpenException.EXCEPTION
        }
        return eventMapper.toEventDetailResponse(host, event)
    }
}
