package band.gosrock.api.v2.ticket.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.api.v2.ticket.dto.response.V2AppliedOptionResponse
import band.gosrock.api.v2.ticket.dto.response.V2TicketAccountResponse
import band.gosrock.api.v2.ticket.dto.response.V2TicketItemManageResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadTicketItemsUseCase(
    private val eventAdaptor: EventAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
) {
    /** 유효(삭제 안 된) 티켓 전체, 생성 순 */
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): List<V2TicketItemManageResponse> = readAll(eventId)

    /** 변경 API 응답용 (권한 검사는 호출한 UseCase 에서 끝남). 변경 트랜잭션이 커밋된 뒤 새로 읽는다 */
    @Transactional(readOnly = true)
    fun readAll(eventId: Long): List<V2TicketItemManageResponse> {
        val event = eventAdaptor.findById(eventId)
        val now = LocalDateTime.now()
        return ticketItemAdaptor.findAllByEventId(eventId).sortedBy { it.id }.map { toResponse(it, event, now) }
    }

    @Transactional(readOnly = true)
    fun readOne(eventId: Long, ticketItemId: Long): V2TicketItemManageResponse =
        toResponse(v2TicketItemDomainService.queryTicketItem(eventId, ticketItemId), eventAdaptor.findById(eventId), LocalDateTime.now())

    private fun toResponse(item: TicketItem, event: Event, now: LocalDateTime): V2TicketItemManageResponse {
        val unlimited = v2TicketItemDomainService.isUnlimitedSupply(item)
        val supplyCount = item.supplyCount!!
        val quantity = item.quantity!!
        return V2TicketItemManageResponse(
            ticketItemId = item.id!!,
            payType = V2TicketPayType.of(item.payType),
            name = item.name,
            description = item.description,
            price = item.price?.longValue() ?: 0L,
            supplyCount = if (unlimited) null else supplyCount,
            remaining = if (unlimited) null else quantity,
            soldCount = supplyCount - quantity,
            approvalRequired = item.type == TicketType.APPROVAL,
            isQuantityPublic = item.isQuantityPublic == true,
            purchaseLimit = if (v2TicketItemDomainService.hasNoPurchaseLimit(item)) null else item.purchaseLimit,
            saleStartAt = item.saleStartAt,
            saleEndAt = item.saleEndAt,
            saleState = v2TicketItemDomainService.saleState(item),
            isSold = item.isSold(),
            isPurchasable = v2TicketItemDomainService.isPurchasable(item, event, now),
            account = item.accountInfo?.takeIf { item.payType == TicketPayType.DUDOONG_TICKET }
                ?.let { V2TicketAccountResponse(bank = it.bankName, holder = it.accountHolder, number = it.accountNumber) },
            options = item.itemOptionGroups.mapNotNull { it.optionGroup }
                .filter { it.optionGroupStatus == OptionGroupStatus.VALID }
                .sortedBy { it.id }
                .map {
                    V2AppliedOptionResponse(
                        optionId = it.id!!,
                        name = it.name,
                        type = V2TicketOptionType.of(it.type),
                        yesAdditionalPrice = v2TicketOptionDomainService.yesAdditionalPrice(it),
                    )
                },
        )
    }
}
