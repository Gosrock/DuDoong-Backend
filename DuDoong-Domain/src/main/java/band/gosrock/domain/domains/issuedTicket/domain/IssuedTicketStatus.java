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
    ENTRANCE_INCOMPLETE("ENTRANCE_INCOMPLETE", "입장 전"),
    // 티켓 취소 (withDraw || cancel)
    CANCELED("CANCELED", "취소 티켓");

    private final String value;

    @JsonValue private final String kr;

    public Boolean isCanceled() {
        return this == IssuedTicketStatus.CANCELED;
    }

    public Boolean isBeforeEntrance() {
        return this == IssuedTicketStatus.ENTRANCE_INCOMPLETE;
    }

    public Boolean isAfterEntrance() {
        return this == IssuedTicketStatus.ENTRANCE_COMPLETED;
    }

    public Boolean is(IssuedTicket issuedTicket) {
        return issuedTicket.getIssuedTicketStatus() == IssuedTicketStatus.ENTRANCE_INCOMPLETE;
    }
}
