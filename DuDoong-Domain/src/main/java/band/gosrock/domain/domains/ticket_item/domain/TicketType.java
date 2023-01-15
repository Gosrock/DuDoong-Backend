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

    /** 결제가 필요한지 상태를 반환하는 메서드 */
    public Boolean isNeedPayment() {
        return this.equals(TicketType.FIRST_COME_FIRST_SERVED);
    }
}
