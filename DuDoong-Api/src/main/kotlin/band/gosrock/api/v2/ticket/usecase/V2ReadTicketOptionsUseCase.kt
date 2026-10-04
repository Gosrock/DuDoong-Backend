package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.api.v2.ticket.dto.response.V2TicketOptionResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadTicketOptionsUseCase(
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
) {
    /** 공연 옵션 풀 (유효 옵션, 생성 순) */
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): List<V2TicketOptionResponse> = readAll(eventId)

    /** 변경 API 응답용 (권한 검사는 호출한 UseCase 에서 끝남) */
    @Transactional(readOnly = true)
    fun readAll(eventId: Long): List<V2TicketOptionResponse> {
        val ticketItems = ticketItemAdaptor.findAllByEventId(eventId)
        val pending = v2TicketItemDomainService.pendingOrderItemIds(ticketItems.mapNotNull { it.id })
        return optionGroupAdaptor.findAllByEventId(eventId).sortedBy { it.id }.map { toResponse(it, ticketItems, pending) }
    }

    @Transactional(readOnly = true)
    fun readOne(eventId: Long, optionGroupId: Long): V2TicketOptionResponse {
        val ticketItems = ticketItemAdaptor.findAllByEventId(eventId)
        val pending = v2TicketItemDomainService.pendingOrderItemIds(ticketItems.mapNotNull { it.id })
        return toResponse(v2TicketOptionDomainService.queryOptionGroup(eventId, optionGroupId), ticketItems, pending)
    }

    private fun toResponse(optionGroup: OptionGroup, ticketItems: List<TicketItem>, pending: Set<Long>): V2TicketOptionResponse =
        V2TicketOptionResponse(
            optionId = optionGroup.id!!,
            name = optionGroup.name,
            description = optionGroup.description,
            type = V2TicketOptionType.of(optionGroup.type),
            yesAdditionalPrice = v2TicketOptionDomainService.yesAdditionalPrice(optionGroup),
            appliedTicketItemIds = v2TicketOptionDomainService.appliedTicketItems(optionGroup, ticketItems).mapNotNull { it.id }.sorted(),
            isLocked = v2TicketOptionDomainService.isLocked(optionGroup, ticketItems, pending),
        )
}
