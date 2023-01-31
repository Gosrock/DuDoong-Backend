package band.gosrock.domain.domains.issuedTicket.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssuedTicketCancelReason {
    REFUND("REFUND", "사용자에 의한 환불");

    private final String value;

    @JsonValue private final String kr;
}
