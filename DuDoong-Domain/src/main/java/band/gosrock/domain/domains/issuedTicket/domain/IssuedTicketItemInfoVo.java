package band.gosrock.domain.domains.issuedTicket.domain;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class IssuedTicketItemInfoVo {

    private Long ticketItemId;

    private TicketType ticketType;

    private String ticketName;

    private Long quantity;

    private Long supplyCount;

    @Builder
    public IssuedTicketItemInfoVo(Long ticketItemId, TicketType ticketType, String ticketName,
        Long quantity, Long supplyCount) {
        this.ticketItemId = ticketItemId;
        this.ticketType = ticketType;
        this.ticketName = ticketName;
        this.quantity = quantity;
        this.supplyCount = supplyCount;
    }

    public static IssuedTicketItemInfoVo from(TicketItem item) {
        return IssuedTicketItemInfoVo.builder().ticketItemId(item.getId())
            .ticketType(item.getType()).ticketName(item.getName())
            .quantity(item.getQuantity()).supplyCount(item.getSupplyCount()).build();
    }
}
