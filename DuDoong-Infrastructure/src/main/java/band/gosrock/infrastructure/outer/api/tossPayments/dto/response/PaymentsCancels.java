package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@NoArgsConstructor
public class PaymentsCancels {
    private Long cancelAmount;
    private String cancelReason;
    private Long taxFreeAmount;
    private Long taxExceptionAmount;
    private Long refundableAmount;

    private Long easyPayDiscountAmount;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private ZonedDateTime canceledAt;

    private String transactionKey;
}
