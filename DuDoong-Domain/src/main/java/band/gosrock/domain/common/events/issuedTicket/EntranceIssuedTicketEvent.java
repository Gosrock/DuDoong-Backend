package band.gosrock.domain.common.events.issuedTicket;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EntranceIssuedTicketEvent extends DomainEvent {

    private final String issuedTicketNo;

    private final Long eventId;

    private final IssuedTicketUserInfoVo userInfo;

    public static EntranceIssuedTicketEvent from(IssuedTicket issuedTicket) {
        return EntranceIssuedTicketEvent.builder()
                .eventId(issuedTicket.getEventId())
                .issuedTicketNo(issuedTicket.getIssuedTicketNo())
                .userInfo(issuedTicket.getUserInfo())
                .build();
    }
}
