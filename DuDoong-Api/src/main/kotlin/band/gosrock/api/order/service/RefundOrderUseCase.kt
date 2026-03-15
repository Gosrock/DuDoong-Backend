package band.gosrock.api.order.service

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.WithdrawOrderService

/** 환불을 위함 환불이라 함은, 사용자가 직접 구매한 물품을 취소시킴 */
@UseCase
class RefundOrderUseCase(
    private val withdrawOrderService: WithdrawOrderService,
    private val orderMapper: OrderMapper,
) {
    fun execute(orderUuid: String): OrderResponse {
        val currentUserId = SecurityUtils.getCurrentUserId()
        withdrawOrderService.refundOrder(orderUuid, currentUserId)
        return orderMapper.toOrderResponse(orderUuid)
    }
}
