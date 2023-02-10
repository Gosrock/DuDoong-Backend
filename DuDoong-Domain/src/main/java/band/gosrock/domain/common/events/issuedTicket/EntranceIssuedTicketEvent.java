package band.gosrock.domain.common.events.issuedTicket;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EntranceIssuedTicketEvent extends DomainEvent {

    private final Long issuedTicketId;

    private final Long eventId;

    public static EntranceIssuedTicketEvent from(IssuedTicket issuedTicket) {
        return EntranceIssuedTicketEvent.builder()
                .eventId(issuedTicket.getEventId())
                .issuedTicketId(issuedTicket.getId())
                .build();
    }
}
