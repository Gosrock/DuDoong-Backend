package band.gosrock.api.order.service

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
    fun execute(userId: Long, orderUuid: String, confirmOrderRequest: ConfirmOrderRequest): OrderResponse {
        val confirmPaymentsRequest = ConfirmPaymentsRequest(
            paymentKey = confirmOrderRequest.paymentKey,
            amount = confirmOrderRequest.amount,
            orderId = orderUuid,
        )
        val confirmOrderUuid = orderConfirmService.execute(confirmPaymentsRequest, userId)
        return orderMapper.toOrderResponse(confirmOrderUuid)
    }
}
