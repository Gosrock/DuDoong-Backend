package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse
import band.gosrock.api.ticketItem.mapper.TicketItemMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class GetEventTicketItemsUseCase(
    private val ticketItemMapper: TicketItemMapper,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
) {

    /** 공개 목록 (비로그인 userId = 0). 준비중 공연은 활성 호스트 멤버만 미리 볼 수 있고 (v1 공연 상세와 같은 기준), 그 외에는 404 */
    @Transactional(readOnly = true)
    fun execute(userId: Long, eventId: Long): GetEventTicketItemsResponse {
        val event = eventAdaptor.findById(eventId)
        if (event.isPreparing() && !hostAdaptor.findById(event.hostId!!).isActiveHostUserId(userId)) {
            throw EventNotFoundException.EXCEPTION
        }
        return ticketItemMapper.toGetEventTicketItemsResponse(eventId, false)
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun executeForAdmin(userId: Long, eventId: Long): GetEventTicketItemsResponse =
        ticketItemMapper.toGetEventTicketItemsResponse(eventId, true)
}
