package band.gosrock.infrastructure.outer.api.tossPayments.client

import band.gosrock.infrastructure.outer.api.tossPayments.config.TransactionGetConfig
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "PaymentsGetClient",
    url = "https://api.tosspayments.com",
    configuration = [TransactionGetConfig::class],
)
interface TransactionGetClient {
    @GetMapping("/v1/payments/orders/{orderId}")
    fun byOrderId(@PathVariable("orderId") orderId: String): PaymentsResponse

    @GetMapping("/v1/payments/{paymentKey}")
    fun byPaymentKey(@PathVariable("paymentKey") paymentKey: String): PaymentsResponse
}
