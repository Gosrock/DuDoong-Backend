package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.request.V2TicketItemRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import java.time.LocalDateTime

@UseCase
class V2CreateTicketItemUseCase(
    private val eventAdaptor: EventAdaptor,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val readTicketItemsUseCase: V2ReadTicketItemsUseCase,
) {
    /** 티켓 없음 공연 400 (Ticket_Item_400_12), PRICE 400 (Ticket_Item_400_11). 등록 후에도 추가 가능 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, request: V2TicketItemRequest): V2TicketItemManageResponse {
        val item = v2TicketItemDomainService.createTicketItem(eventAdaptor.findById(eventId), request.toForm(), LocalDateTime.now())
        return readTicketItemsUseCase.readOne(eventId, item.id!!)
    }
}
