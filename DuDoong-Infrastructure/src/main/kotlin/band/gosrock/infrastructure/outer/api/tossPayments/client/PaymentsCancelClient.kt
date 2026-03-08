package band.gosrock.infrastructure.outer.api.tossPayments.client

import band.gosrock.infrastructure.outer.api.tossPayments.config.PaymentsCancelConfig
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(
    name = "PaymentsCancelClient",
    url = "\${feign.toss.url}",
    configuration = [PaymentsCancelConfig::class],
)
interface PaymentsCancelClient {
    @PostMapping("/v1/payments/{paymentKey}/cancel")
    fun execute(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @PathVariable("paymentKey") paymentKey: String,
        @RequestBody cancelPaymentsRequest: CancelPaymentsRequest,
    ): PaymentsResponse
}
