package band.gosrock.domain.domains.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.Getter;

@Getter
public class IssuedTicketDTO {

    private final IssuedTicketInfoVo issuedTicketInfo;

    private final EventInfoVo eventInfo;

    public IssuedTicketDTO(IssuedTicket issuedTicket) {
        this.issuedTicketInfo = issuedTicket.toIssuedTicketInfoVo(issuedTicket);
        this.eventInfo = new EventInfoVo(issuedTicket.getEvent());
    }
}
