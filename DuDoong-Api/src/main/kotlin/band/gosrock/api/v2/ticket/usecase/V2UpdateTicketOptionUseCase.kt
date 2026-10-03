package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.request.V2UpdateTicketOptionRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketOptionResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService

@UseCase
class V2UpdateTicketOptionUseCase(
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
    private val readTicketOptionsUseCase: V2ReadTicketOptionsUseCase,
) {
    /** 부분 수정. 판매된 티켓에 붙은 옵션은 이름·설명만 (Option_Group_400_5, DEC-012) */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, optionGroupId: Long, request: V2UpdateTicketOptionRequest): V2TicketOptionResponse {
        v2TicketOptionDomainService.updateOptionGroup(eventId, optionGroupId, request.name, request.description, request.yesAdditionalPrice)
        return readTicketOptionsUseCase.readOne(eventId, optionGroupId)
    }
}
