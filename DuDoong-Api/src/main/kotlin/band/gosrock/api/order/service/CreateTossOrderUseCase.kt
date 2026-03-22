package band.gosrock.api.order.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCreateClient
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class CreateTossOrderUseCase(
    private val paymentsCreateClient: PaymentsCreateClient,
    private val orderAdaptor: OrderAdaptor,
) {
    private val log = LoggerFactory.getLogger(CreateTossOrderUseCase::class.java)

    fun execute(orderUuid: String): PaymentsResponse {
        log.info("[CreateTossOrderUseCase][execute] Toss 결제 생성 orderUuid={}", orderUuid)
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
