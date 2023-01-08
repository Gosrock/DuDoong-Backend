package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentsCashReceipt {
    private String receiptKey;
    // 스독공제 지출증빙
    private String type;
    private Long amount;
    private Long taxFreeAmount;
    private String issueNumber;
    private String receiptUrl;
}
