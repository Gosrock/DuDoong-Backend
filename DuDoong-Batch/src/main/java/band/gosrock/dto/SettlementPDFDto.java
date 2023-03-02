package band.gosrock.dto;


import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SettlementPDFDto {
    private final String eventTitle;
    private final String hostName;
    private final LocalDateTime settlementAt;
    private final String dudoongTicketAmount;
    private final String pgTicketAmount;
    private final String totalAmount;
    private final String dudoongFee;
    private final String pgFee;
    private final String totalFee;
    private final String totalFeeVat;
    private final String totalSettlement;
    private final LocalDateTime now;
}
