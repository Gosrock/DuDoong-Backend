package band.gosrock.api.order.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.WithdrawOrderService
import org.slf4j.LoggerFactory

@UseCase
class CancelOrderUseCase(
    private val withdrawOrderService: WithdrawOrderService,
    private val orderMapper: OrderMapper,
) {
    private val log = LoggerFactory.getLogger(CancelOrderUseCase::class.java)

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, orderUuid: String, reason: String? = null): OrderResponse {
        log.info("[CancelOrderUseCase][execute] 주문 취소 userId={} eventId={} orderUuid={} reason={}", userId, eventId, orderUuid, reason)
        withdrawOrderService.cancelOrder(orderUuid, reason)
        return orderMapper.toOrderResponse(orderUuid)
    }
}
