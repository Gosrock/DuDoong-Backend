package band.gosrock.api.order.service

import band.gosrock.api.order.model.dto.request.CreateOrderRequest
import band.gosrock.api.order.model.dto.response.CreateOrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.CreateOrderService
import org.slf4j.LoggerFactory

@UseCase
class CreateOrderUseCase(
    private val createOrderService: CreateOrderService,
    private val orderMapper: OrderMapper,
) {
    private val log = LoggerFactory.getLogger(CreateOrderUseCase::class.java)

    fun execute(userId: Long, createOrderRequest: CreateOrderRequest): CreateOrderResponse {
        log.info("[CreateOrderUseCase][execute] 주문 생성 userId={} cartId={} couponId={}", userId, createOrderRequest.cartId, createOrderRequest.couponId)
        val couponId = createOrderRequest.couponId
        val cartId = createOrderRequest.cartId!!
        return if (couponId == null) {
            orderMapper.toCreateOrderResponse(createOrderService.withOutCoupon(cartId, userId))
        } else {
            orderMapper.toCreateOrderResponse(createOrderService.withCoupon(cartId, userId, couponId))
        }
    }
}
