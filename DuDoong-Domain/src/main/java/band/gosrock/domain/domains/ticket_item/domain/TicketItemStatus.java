package band.gosrock.domain.domains.ticket_item.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketItemStatus {
    // 유효
    VALID("VALID", "유효"),
    // 삭제됨
    DELETED("DELETED", "삭제됨");

    private final String value;

    @JsonValue private final String kr;
}
