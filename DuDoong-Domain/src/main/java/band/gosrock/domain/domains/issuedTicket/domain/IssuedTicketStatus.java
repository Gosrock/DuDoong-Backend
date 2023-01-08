package band.gosrock.domain.domains.issuedTicket.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssuedTicketStatus {
    // 입장 완료
    ENTRANCE_COMPLETED("ENTRANCE_COMPLETED"),
    // 입장 미완료
    ENTRANCE_INCOMPLETE("ENTRANCE_INCOMPLETE");

    private final String value;
}
