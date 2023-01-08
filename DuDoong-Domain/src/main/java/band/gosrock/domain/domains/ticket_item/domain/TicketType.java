package band.gosrock.domain.domains.ticket_item.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketType {
    // 선착순
    FIRST_COME_FIRST_SERVED("FIRST_COME_FIRST_SERVED"),
    // 승인
    APPROVAL("APPROVAL");
    private String value;
}
