package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.request.V2ReplaceTicketOptionsRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService

@UseCase
class V2ReplaceTicketItemOptionsUseCase(
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val readTicketItemsUseCase: V2ReadTicketItemsUseCase,
) {
    /** 옵션 전체 지정. 판매된 티켓은 변경 불가(같은 목록은 허용), 다른 공연 옵션 400, 무료티켓에 유료 옵션 400 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, ticketItemId: Long, request: V2ReplaceTicketOptionsRequest): V2TicketItemManageResponse {
        v2TicketItemDomainService.replaceOptions(eventId, ticketItemId, request.optionIds!!)
        return readTicketItemsUseCase.readOne(eventId, ticketItemId)
    }
}
