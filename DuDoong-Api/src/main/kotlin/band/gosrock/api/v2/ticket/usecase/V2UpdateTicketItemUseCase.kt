package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.request.V2UpdateTicketItemRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import java.time.LocalDateTime

@UseCase
class V2UpdateTicketItemUseCase(
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val readTicketItemsUseCase: V2ReadTicketItemsUseCase,
) {
    /** 부분 수정 (null = 변경 안 함, '값 없음'은 clear, #755). 판매된 티켓은 DEC-006 허용 필드만 (Ticket_Item_400_14). 재고 감소와 같은 락 안에서 처리 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, ticketItemId: Long, request: V2UpdateTicketItemRequest): V2TicketItemManageResponse {
        v2TicketItemDomainService.updateTicketItem(eventId, ticketItemId, request.toPatch(), LocalDateTime.now())
        return readTicketItemsUseCase.readOne(eventId, ticketItemId)
    }
}
