package band.gosrock.domain.domains.issuedTicket.domain;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class IssuedTicketItemInfoVo {

    private Long ticketItemId;

    @Enumerated(EnumType.STRING)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    private TicketPayType payType;

    private String ticketName;

    private Money price;

    @Builder
    public IssuedTicketItemInfoVo(
            Long ticketItemId,
            TicketType ticketType,
            TicketPayType payType,
            String ticketName,
            Money price) {
        this.ticketItemId = ticketItemId;
        this.ticketType = ticketType;
        this.payType = payType;
        this.ticketName = ticketName;
        this.price = price;
    }

    public static IssuedTicketItemInfoVo from(TicketItem item) {
        return IssuedTicketItemInfoVo.builder()
                .ticketItemId(item.getId())
                .ticketType(item.getType())
                .payType(item.getPayType())
                .ticketName(item.getName())
                .price(item.getPrice())
                .build();
    }
}
