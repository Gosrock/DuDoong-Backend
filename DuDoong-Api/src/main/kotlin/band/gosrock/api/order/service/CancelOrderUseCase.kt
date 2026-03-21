package band.gosrock.api.order.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.WithdrawOrderService

@UseCase
class CancelOrderUseCase(
    private val withdrawOrderService: WithdrawOrderService,
    private val orderMapper: OrderMapper,
) {
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, orderUuid: String): OrderResponse {
        withdrawOrderService.cancelOrder(orderUuid)
        return orderMapper.toOrderResponse(orderUuid)
    }
}
