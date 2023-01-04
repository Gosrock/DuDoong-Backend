package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentsCard {
    private Long amount;
    private String issuerCode;
    private String acquirerCode;
    private String number;
    private Long installmentPlanMonths;
    private String approveNo;
    private Boolean useCardPoint;
    private String cardType;
    private String ownerType;
    private String acquireStatus;

    private Boolean isInterestFree;
    private String interestPayer;
}
