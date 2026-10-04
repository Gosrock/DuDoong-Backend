package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.event.dto.response.V2PublicTicketItemResponse
import band.gosrock.api.v2.event.dto.response.V2PublicTicketOptionResponse
import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseDomainService
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemQuery
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadOnSaleTicketItemsUseCase(
    private val eventAdaptor: EventAdaptor,
    private val v2TicketItemQuery: V2TicketItemQuery,
    private val v2EventBrowseDomainService: V2EventBrowseDomainService,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
) {
    /**
     * 공개 API. 판매 중(`TicketItem.isOnSale`: 판매 중단 아님 + 판매 기간 안)인 유효 티켓만, 생성 순 (DEC-020).
     * 준비중·삭제 공연은 404. 지난 공연도 목록은 보이고 isPurchasable=false. 입금 계좌는 내려주지 않는다 (주문 단계에서 제공)
     */
    @Transactional(readOnly = true)
    fun execute(eventId: Long): List<V2PublicTicketItemResponse> {
        val event = eventAdaptor.findById(eventId)
        v2EventBrowseDomainService.validatePublic(event)
        val now = LocalDateTime.now()
        // 티켓·옵션 그룹 fetch join (옵션 N+1 방지)
        return v2TicketItemQuery.findValidWithOptionGroupsByEventId(eventId)
            .filter { it.isOnSale(now) }
            .map { toResponse(it, event, now) }
    }

    private fun toResponse(item: TicketItem, event: Event, now: LocalDateTime): V2PublicTicketItemResponse =
        V2PublicTicketItemResponse(
            ticketItemId = item.id!!,
            name = item.name,
            description = item.description,
            price = item.price?.longValue() ?: 0L,
            payType = V2TicketPayType.of(item.payType),
            approvalRequired = item.type == TicketType.APPROVAL,
            remaining = item.quantity?.takeIf { item.isQuantityPublic == true && !item.isUnlimitedSupply() },
            isSoldOut = !item.isQuantityLeft(),
            isPurchasable = v2TicketItemDomainService.isPurchasableInV2App(item, event, now),
            purchaseLimit = if (item.hasNoPurchaseLimit()) null else item.purchaseLimit,
            options = item.itemOptionGroups.mapNotNull { it.optionGroup }
                .filter { it.optionGroupStatus == OptionGroupStatus.VALID }
                .sortedBy { it.id }
                .map {
                    V2PublicTicketOptionResponse(
                        optionId = it.id!!,
                        name = it.name,
                        description = it.description,
                        type = V2TicketOptionType.of(it.type),
                        yesAdditionalPrice = v2TicketOptionDomainService.yesAdditionalPrice(it),
                    )
                },
        )
}
