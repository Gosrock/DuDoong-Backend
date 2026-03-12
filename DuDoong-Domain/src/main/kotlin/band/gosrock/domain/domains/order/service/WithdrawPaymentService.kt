package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCancelClient
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse
import org.slf4j.LoggerFactory

@DomainService
class WithdrawPaymentService(private val paymentsCancelClient: PaymentsCancelClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(orderUuid: String, paymentKey: String, reason: String): PaymentsResponse {
        log.info("취소처리 $orderUuid : $paymentKey$reason")
        return paymentsCancelClient.execute(
            orderUuid,
            paymentKey,
            CancelPaymentsRequest.builder().cancelReason(reason).build(),
        )
    }
}
