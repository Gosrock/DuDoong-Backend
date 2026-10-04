package band.gosrock.api.v2.operation.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.operation.dto.response.V2DashboardOrdersResponse
import band.gosrock.api.v2.operation.dto.response.V2DashboardResponse
import band.gosrock.api.v2.operation.dto.response.V2DashboardTicketItemResponse
import band.gosrock.api.v2.operation.dto.response.V2DashboardTicketsResponse
import band.gosrock.api.v2.operation.dto.response.V2EntranceStatsResponse
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketQuery
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketSearch
import band.gosrock.domain.domains.order.service.v2.V2OrderQuery
import band.gosrock.domain.domains.order.service.v2.V2OrderSearch
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService

/** D-1 대시보드 (일반 멤버 이상) */
@UseCase
class V2ReadDashboardUseCase(
    private val eventAdaptor: EventAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val v2OrderQuery: V2OrderQuery,
    private val v2IssuedTicketQuery: V2IssuedTicketQuery,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
) {
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): V2DashboardResponse {
        eventAdaptor.findById(eventId)
        val counts = v2OrderQuery.counts(V2OrderSearch(eventId = eventId))
        // 판매 매수 = 재고 감소량 (승인·결제 완료로 발급, 취소 시 복구). 무제한은 판매 수량 null
        val ticketItems = ticketItemAdaptor.findAllByEventId(eventId).sortedBy { it.id }
        // 승인 대기 수량 그룹 쿼리 1개 (T-1·P-5 와 같은 쿼리)
        val pending = v2TicketItemDomainService.pendingApproveQuantities(ticketItems.mapNotNull { it.id })
        val items = ticketItems.map {
            V2DashboardTicketItemResponse(
                ticketItemId = it.id!!,
                name = it.name,
                payType = V2TicketPayType.of(it.payType),
                soldCount = it.supplyCount!! - it.quantity!!,
                supplyCount = if (it.isUnlimitedSupply()) null else it.supplyCount,
                pendingApproveCount = pending[it.id] ?: 0L,
            )
        }
        return V2DashboardResponse(
            orders = V2DashboardOrdersResponse(
                pendingApprove = counts.pendingApprove,
                approved = counts.approved,
                refused = counts.refused,
                refundRequested = v2OrderQuery.countRefundRequested(eventId),
            ),
            tickets = V2DashboardTicketsResponse(
                totalSoldCount = items.sumOf { it.soldCount },
                totalSupplyCount = if (items.any { it.supplyCount == null }) null else items.sumOf { it.supplyCount!! },
                items = items,
            ),
            salesAmount = v2OrderQuery.sumSalesAmount(eventId),
            entrance = V2EntranceStatsResponse.of(v2IssuedTicketQuery.stats(V2IssuedTicketSearch(eventId = eventId))),
        )
    }
}
