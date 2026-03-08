package band.gosrock.infrastructure.outer.api.tossPayments.client

import band.gosrock.infrastructure.outer.api.tossPayments.config.TransactionGetConfig
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@FeignClient(
    name = "SettlementClient",
    url = "\${feign.toss.url}",
    configuration = [TransactionGetConfig::class],
)
interface SettlementClient {
    @GetMapping("/v1/settlements")
    fun execute(
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam("dateType") dateType: String,
        @RequestParam("page") page: Int,
        @RequestParam("size") size: Int,
    ): List<SettlementResponse>
}
