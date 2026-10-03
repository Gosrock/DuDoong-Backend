package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService

@UseCase
class V2DeleteTicketItemUseCase(
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val readTicketItemsUseCase: V2ReadTicketItemsUseCase,
) {
    /** 잠기지 않은 티켓만 (재고 감소 또는 승인 대기 주문이면 Ticket_Item_400_7). 남은 티켓 목록을 돌려준다 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, ticketItemId: Long): List<V2TicketItemManageResponse> {
        v2TicketItemDomainService.deleteTicketItem(eventId, ticketItemId)
        return readTicketItemsUseCase.readAll(eventId)
    }
}
