package band.gosrock.domain.common.vo;

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.Getter;

@Getter
public class IssuedTicketInfoVo {

    /*
    발급 티켓 id
     */
    private final Long issuedTicketId;

    /*
    발급 티켓 번호 Ex. T10000001
     */
    private final String issuedTicketNo;

    /*
    발급 티켓 uuid
     */
    private final String uuid;

    /*
    발급 티켓 종류
     */
    private final String ticketName;

    /*
    발급 티켓 가격
     */
    private final Long ticketAccount;


    public IssuedTicketInfoVo(IssuedTicket issuedTicket) {
        this.issuedTicketId = issuedTicket.getId();
        this.issuedTicketNo = issuedTicket.getIssuedTicketNo();
        this.uuid = issuedTicket.getUuid();
        this.ticketName = issuedTicket.getTicketItem().getName();
        this.ticketAccount = issuedTicket.getPrice();
    }
}
