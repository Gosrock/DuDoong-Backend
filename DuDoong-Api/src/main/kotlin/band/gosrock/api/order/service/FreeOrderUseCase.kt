package band.gosrock.api.order.service

import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.FreeOrderService
import org.slf4j.LoggerFactory

@UseCase
class FreeOrderUseCase(
    private val freeOrderService: FreeOrderService,
    private val orderMapper: OrderMapper,
) {
    private val log = LoggerFactory.getLogger(FreeOrderUseCase::class.java)

    fun execute(userId: Long, orderUuid: String): OrderResponse {
        log.info("[FreeOrderUseCase][execute] 무료 주문 확정 userId={} orderUuid={}", userId, orderUuid)
        val confirmOrderUuid = freeOrderService.execute(orderUuid, userId)
        return orderMapper.toOrderResponse(confirmOrderUuid)
    }
}
