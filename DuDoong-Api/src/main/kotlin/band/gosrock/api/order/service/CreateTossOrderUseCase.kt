package band.gosrock.api.order.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCreateClient
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class CreateTossOrderUseCase(
    private val paymentsCreateClient: PaymentsCreateClient,
    private val orderAdaptor: OrderAdaptor,
) {
    fun execute(orderUuid: String): PaymentsResponse {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val createPaymentsRequest = CreatePaymentsRequest(
            method = "카드",
            orderName = order.orderName,
            orderId = orderUuid,
            failUrl = "http://localhost:8080/failurl",
            successUrl = "http://localhost:8080/successUrl",
            amount = order.getTotalPaymentPrice().longValue(),
        )
        return paymentsCreateClient.execute(createPaymentsRequest)
    }
}
