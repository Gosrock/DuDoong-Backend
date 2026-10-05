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
        if (couponId != null) return orderMapper.toCreateOrderResponse(createOrderService.withCoupon(cartId, userId, couponId))
        // 승인형은 v2 주문과 같은 티켓 락, 그 외는 예전 사용자 락 (#724). 락 키는 락 전에 읽는다
        val key = createOrderService.lockKeyOf(cartId, userId)
        val orderUuid = if (key.approval) createOrderService.withOutCouponApproval(key.itemId, cartId, userId) else createOrderService.withOutCoupon(cartId, userId)
        return orderMapper.toCreateOrderResponse(orderUuid)
    }
}
