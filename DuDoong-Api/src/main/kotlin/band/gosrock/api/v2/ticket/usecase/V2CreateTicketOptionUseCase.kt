package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.request.V2CreateTicketOptionRequest
import band.gosrock.api.v2.ticket.dto.response.V2TicketOptionResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService

@UseCase
class V2CreateTicketOptionUseCase(
    private val eventAdaptor: EventAdaptor,
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
    private val readTicketOptionsUseCase: V2ReadTicketOptionsUseCase,
) {
    /** 주관식 / 네·아니오. 추가 금액은 네·아니오만 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, request: V2CreateTicketOptionRequest): V2TicketOptionResponse {
        val optionGroup = v2TicketOptionDomainService.createOptionGroup(
            event = eventAdaptor.findById(eventId),
            name = request.name!!,
            description = request.description!!,
            type = request.type!!.domain,
            yesAdditionalPrice = request.yesAdditionalPrice,
        )
        return readTicketOptionsUseCase.readOne(eventId, optionGroup.id!!)
    }
}
