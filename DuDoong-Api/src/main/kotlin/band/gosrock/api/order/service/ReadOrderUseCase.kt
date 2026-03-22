package band.gosrock.api.order.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.common.page.PageResponse
import band.gosrock.api.common.slice.SliceResponse
import band.gosrock.api.order.model.dto.request.AdminOrderTableQueryRequest
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement
import band.gosrock.api.order.model.dto.response.OrderBriefElement
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.dto.response.OrderTicketResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class ReadOrderUseCase(
    private val orderMapper: OrderMapper,
    private val orderAdaptor: OrderAdaptor,
    private val orderValidator: OrderValidator,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
) {
    fun getOrderDetail(userId: Long, orderUuid: String): OrderResponse {
        val order = getMyOrder(userId, orderUuid)
        return orderMapper.toOrderResponse(order)
    }

    private fun getMyOrder(userId: Long, orderUuid: String): Order {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        orderValidator.validOwner(order, userId)
        return order
    }

    fun getRecentOrder(userId: Long): OrderBriefElement? {
        val recentOrder = orderAdaptor.findRecentOrderByUserId(userId)
        return recentOrder.map { orderMapper.toOrderBriefElement(it) }.orElse(null)
    }

    fun getMyOrders(userId: Long, showing: Boolean, pageable: Pageable): SliceResponse<OrderBriefElement> {
        val condition = if (showing == true) {
            FindMyPageOrderCondition.onShowing(userId)
        } else {
            FindMyPageOrderCondition.notShowing(userId)
        }
        val ordersWithPagination = orderAdaptor.findMyOrders(condition, pageable)
        val orderBriefElements = orderMapper.toOrderBriefsResponse(ordersWithPagination)
        return SliceResponse.of(orderBriefElements)
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun getEventOrders(
        userId: Long,
        eventId: Long,
        adminOrderTableQueryRequest: AdminOrderTableQueryRequest,
        pageable: Pageable,
    ): PageResponse<OrderAdminTableElement> {
        val orders = orderAdaptor.findEventOrders(adminOrderTableQueryRequest.toCondition(eventId), pageable)
        return PageResponse.of(orderMapper.toOrderAdminTableElement(eventId, orders))
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun getEventOrderDetail(userId: Long, eventId: Long, orderUuid: String): OrderResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        return orderMapper.toOrderResponse(order)
    }

    fun getOrderTickets(userId: Long, orderUuid: String): OrderTicketResponse {
        val order = getMyOrder(userId, orderUuid)
        return orderMapper.toOrderTicketResponse(order)
    }
}
