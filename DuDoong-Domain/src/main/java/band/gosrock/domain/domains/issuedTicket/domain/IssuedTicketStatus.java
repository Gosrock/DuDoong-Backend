package band.gosrock.domain.domains.issuedTicket.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssuedTicketStatus {
    // 입장 완료
    ENTRANCE_COMPLETED("ENTRANCE_COMPLETED", "입장 완료"),
    // 입장 미완료
    ENTRANCE_INCOMPLETE("ENTRANCE_INCOMPLETE", "입장 전");

    private final String value;

    @JsonValue private final String kr;
}
