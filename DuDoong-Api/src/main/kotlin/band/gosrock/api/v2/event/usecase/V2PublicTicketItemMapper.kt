package band.gosrock.api.v2.event.usecase

import band.gosrock.api.v2.event.dto.response.V2PublicTicketItemResponse
import band.gosrock.api.v2.event.dto.response.V2PublicTicketOptionResponse
import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
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
import org.springframework.stereotype.Component

/** 공개 공연의 판매 중 티켓과 사용자용 응답 (P-5 와 O-0 결제 화면 공통, #726). 계좌는 다루지 않는다 */
data class V2PublicTicketItem(val item: TicketItem, val response: V2PublicTicketItemResponse)

@Component
class V2PublicTicketItemMapper(
    private val eventAdaptor: EventAdaptor,
    private val v2TicketItemQuery: V2TicketItemQuery,
    private val v2EventBrowseDomainService: V2EventBrowseDomainService,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val v2TicketOptionDomainService: V2TicketOptionDomainService,
) {
    /**
     * 판매 중(`TicketItem.isOnSale`: 판매 중단 아님 + 판매 기간 안)인 유효 티켓, 생성 순 (DEC-020). 준비중·삭제 공연은 404.
     * 잔여·매진·구매 가능은 승인 대기 수량을 뺀 값 (#726, 티켓별 합계는 한 번의 그룹 쿼리). 호출 측 트랜잭션 안에서 부른다
     */
    fun onSaleItems(eventId: Long): List<V2PublicTicketItem> {
        val event = eventAdaptor.findById(eventId)
        v2EventBrowseDomainService.validatePublic(event)
        val now = LocalDateTime.now()
        // 티켓·옵션 그룹 fetch join (옵션 N+1 방지)
        val items = v2TicketItemQuery.findValidWithOptionGroupsByEventId(eventId).filter { it.isOnSale(now) }
        val pending = v2TicketItemDomainService.pendingApproveQuantities(items.map { it.id!! })
        return items.map { V2PublicTicketItem(it, toResponse(it, event, now, pending[it.id] ?: 0L)) }
    }

    private fun toResponse(item: TicketItem, event: Event, now: LocalDateTime, pendingApproveQuantity: Long): V2PublicTicketItemResponse {
        val available = v2TicketItemDomainService.availableQuantity(item, pendingApproveQuantity)
        return V2PublicTicketItemResponse(
            ticketItemId = item.id!!,
            name = item.name,
            description = item.description,
            price = item.price?.longValue() ?: 0L,
            payType = V2TicketPayType.of(item.payType),
            approvalRequired = item.type == TicketType.APPROVAL,
            remaining = available.takeIf { item.isQuantityPublic == true && !item.isUnlimitedSupply() },
            isSoldOut = available <= 0,
            isPurchasable = v2TicketItemDomainService.isPurchasableInV2App(item, event, now, pendingApproveQuantity),
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
}
