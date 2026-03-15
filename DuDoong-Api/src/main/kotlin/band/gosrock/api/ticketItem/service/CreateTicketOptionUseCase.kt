package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.service.TicketOptionService

@UseCase
class CreateTicketOptionUseCase(
    private val ticketOptionMapper: TicketOptionMapper,
    private val ticketOptionService: TicketOptionService,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(createTicketOptionRequest: CreateTicketOptionRequest, eventId: Long): OptionGroupResponse {
        val ticketOption = ticketOptionMapper
            .toOptionGroup(createTicketOptionRequest, eventId)
            .createTicketOption(Money.wons(createTicketOptionRequest.additionalPrice!!))
        val ticketOptionResult = ticketOptionService.createTicketOption(ticketOption)
        return OptionGroupResponse.from(ticketOptionResult)
    }
}
