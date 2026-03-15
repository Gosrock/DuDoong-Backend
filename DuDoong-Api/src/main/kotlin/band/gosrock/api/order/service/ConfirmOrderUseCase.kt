package band.gosrock.api.order.service

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest
import band.gosrock.api.order.model.dto.response.OrderResponse
import band.gosrock.api.order.model.mapper.OrderMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.service.OrderConfirmService
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest

@UseCase
class ConfirmOrderUseCase(
    private val orderConfirmService: OrderConfirmService,
    private val orderMapper: OrderMapper,
) {
    fun execute(orderUuid: String, confirmOrderRequest: ConfirmOrderRequest): OrderResponse {
        val currentUserId = SecurityUtils.getCurrentUserId()
        val confirmPaymentsRequest = ConfirmPaymentsRequest.builder()
            .paymentKey(confirmOrderRequest.paymentKey)
            .amount(confirmOrderRequest.amount)
            .orderId(orderUuid)
            .build()
        val confirmOrderUuid = orderConfirmService.execute(confirmPaymentsRequest, currentUserId)
        return orderMapper.toOrderResponse(confirmOrderUuid)
    }
}
