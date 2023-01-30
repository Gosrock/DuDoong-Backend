package band.gosrock.domain.domains.issuedTicket.domain;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Embeddable
@Builder
@RequiredArgsConstructor
public class IssuedTicketItemInfoVo {

    private final Long ticketItemId;

    private final TicketType ticketType;

    private final String ticketName;

    private final Long quantity;

    private final Long supplyCount;

    public static IssuedTicketItemInfoVo from(TicketItem item) {
        return IssuedTicketItemInfoVo.builder().ticketItemId(item.getId())
            .ticketType(item.getType()).ticketName(item.getName())
            .quantity(item.getQuantity()).supplyCount(item.getSupplyCount()).build();
    }
}
