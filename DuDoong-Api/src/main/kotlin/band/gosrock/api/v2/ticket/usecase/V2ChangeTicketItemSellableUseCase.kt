package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService

@UseCase
class V2ChangeTicketItemSellableUseCase(
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val readTicketItemsUseCase: V2ReadTicketItemsUseCase,
) {
    /** 판매 중단(false) / 재개(true). 이미 그 상태면 그대로 200 (멱등) */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, ticketItemId: Long, sellable: Boolean): V2TicketItemManageResponse {
        v2TicketItemDomainService.changeSellable(eventId, ticketItemId, sellable)
        return readTicketItemsUseCase.readOne(eventId, ticketItemId)
    }
}
