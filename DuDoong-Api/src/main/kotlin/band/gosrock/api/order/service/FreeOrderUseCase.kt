package band.gosrock.api.order.service

import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.FreeOrderService

@UseCase
class FreeOrderUseCase(
    private val freeOrderService: FreeOrderService,
    private val orderMapper: OrderMapper,
) {
    fun execute(userId: Long, orderUuid: String): OrderResponse {
        val confirmOrderUuid = freeOrderService.execute(orderUuid, userId)
        return orderMapper.toOrderResponse(confirmOrderUuid)
    }
}
