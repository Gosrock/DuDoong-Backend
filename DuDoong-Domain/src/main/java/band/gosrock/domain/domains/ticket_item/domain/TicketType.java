package band.gosrock.domain.domains.ticket_item.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketType {
    // 선착순
    FIRST_COME_FIRST_SERVED("FIRST_COME_FIRST_SERVED","결제방식"),
    // 승인
    APPROVAL("APPROVAL","승인방식");
    private String value;

    @JsonValue
    private String kr;

    /** 결제가 필요한지 상태를 반환하는 메서드 */
    public Boolean isNeedPayment() {
        return this.equals(TicketType.FIRST_COME_FIRST_SERVED);
    }
}
