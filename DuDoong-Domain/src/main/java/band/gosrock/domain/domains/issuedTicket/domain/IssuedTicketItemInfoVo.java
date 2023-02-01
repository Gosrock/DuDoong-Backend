package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class IssuedTicketItemInfoVo {

    private Long ticketItemId;

    private TicketType ticketType;

    private String ticketName;

    @Builder
    public IssuedTicketItemInfoVo(Long ticketItemId, TicketType ticketType, String ticketName) {
        this.ticketItemId = ticketItemId;
        this.ticketType = ticketType;
        this.ticketName = ticketName;
    }

    public static IssuedTicketItemInfoVo from(TicketItem item) {
        return IssuedTicketItemInfoVo.builder()
                .ticketItemId(item.getId())
                .ticketType(item.getType())
                .ticketName(item.getName())
                .build();
    }
}
