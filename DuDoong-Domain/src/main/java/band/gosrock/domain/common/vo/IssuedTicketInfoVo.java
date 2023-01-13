package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
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
    private final Money ticketPrice;

    /*
    티켓 발급 시간
     */
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd hh:mm:ss",
            timezone = "Asia/Seoul")
    private final LocalDateTime createdAt;

    /*
    발급 티켓 상태
     */
    private final IssuedTicketStatus issuedTicketStatus;

    /*
    발급 티켓 옵션 금액 합계
     */
    private final Money optionPrice;

    public IssuedTicketInfoVo(IssuedTicket issuedTicket) {
        this.issuedTicketId = issuedTicket.getId();
        this.issuedTicketNo = issuedTicket.getIssuedTicketNo();
        this.uuid = issuedTicket.getUuid();
        this.ticketName = issuedTicket.getTicketItem().getName();
        this.ticketPrice = issuedTicket.getTicketItem().getPrice();
        this.createdAt = issuedTicket.getCreatedAt();
        this.issuedTicketStatus = issuedTicket.getIssuedTicketStatus();
        this.optionPrice = issuedTicket.sumOptionPrice();
    }
}
