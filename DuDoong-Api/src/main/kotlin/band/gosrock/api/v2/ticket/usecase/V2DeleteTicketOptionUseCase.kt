package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.response.V2TicketOptionResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService

@UseCase
class V2DeleteTicketOptionUseCase(
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
    private val readTicketOptionsUseCase: V2ReadTicketOptionsUseCase,
) {
    /** 판매된 티켓에 붙어 있으면 400 (Option_Group_400_2). 판매 전 티켓에서는 떼어 내고 삭제. 남은 옵션 목록을 돌려준다 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, optionGroupId: Long): List<V2TicketOptionResponse> {
        v2TicketOptionDomainService.deleteOptionGroup(eventId, optionGroupId)
        return readTicketOptionsUseCase.readAll(eventId)
    }
}
