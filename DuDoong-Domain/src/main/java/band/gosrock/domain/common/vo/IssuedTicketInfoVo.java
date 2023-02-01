package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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
    @DateFormat private final LocalDateTime createdAt;

    /*
    발급 티켓 상태
     */
    private final IssuedTicketStatus issuedTicketStatus;

    /*
    발급 티켓 옵션 금액 합계
     */
    private final Money optionPrice;

    private final IssuedTicketUserInfoVo userInfo;

    public static IssuedTicketInfoVo from(IssuedTicket issuedTicket) {
        return IssuedTicketInfoVo.builder()
                .issuedTicketId(issuedTicket.getId())
                .issuedTicketNo(issuedTicket.getIssuedTicketNo())
                .uuid(issuedTicket.getUuid())
                .ticketName(issuedTicket.getItemInfo().getTicketName())
                .ticketPrice(issuedTicket.getPrice())
                .createdAt(issuedTicket.getCreatedAt())
                .issuedTicketStatus(issuedTicket.getIssuedTicketStatus())
                .optionPrice(issuedTicket.sumOptionPrice())
                .userInfo(issuedTicket.getUserInfo())
                .build();
    }
}
