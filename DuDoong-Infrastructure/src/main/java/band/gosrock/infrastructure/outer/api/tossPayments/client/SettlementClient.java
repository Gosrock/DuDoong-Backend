package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.outer.api.tossPayments.config.TransactionGetConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "SettlementClient",
        url = "${feign.toss.url}",
        configuration = {TransactionGetConfig.class})
public interface SettlementClient {
    @GetMapping("/v1/settlements")
    List<SettlementResponse> execute(
            @RequestParam(value = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(value = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(value = "dateType") String dateType,
            @RequestParam(value = "page") int page,
            @RequestParam(value = "size") int size);
}
