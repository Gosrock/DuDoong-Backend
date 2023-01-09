package band.gosrock.domain.domains.issuedTicket.dto.response;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.Getter;

@Getter
public class IssuedTicketDTO {

    private final Long issuedTicketId;

    private final String uuid;

    public IssuedTicketDTO(IssuedTicket issuedTicket) {
        this.issuedTicketId = issuedTicket.getId();
        this.uuid = issuedTicket.getUuid();
    }
}
