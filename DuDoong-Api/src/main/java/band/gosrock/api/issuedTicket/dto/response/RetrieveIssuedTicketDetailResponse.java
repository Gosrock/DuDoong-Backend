package band.gosrock.api.issuedTicket.dto.response;

import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.response.IssuedTicketDTO;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RetrieveIssuedTicketDetailResponse {


    /*
    발급 티켓 정보 VO
     */
    private final IssuedTicketInfoVo issuedTicketInfo;

    /*
    이벤트 정보 VO
     */
    private final EventInfoVo eventInfo;

    /*
    발급 유저 이름
     */
    private final String userName;

    public RetrieveIssuedTicketDetailResponse(IssuedTicketDTO issuedTicket, User user) {
        this.issuedTicketInfo = issuedTicket.getIssuedTicketInfo();
        this.eventInfo = issuedTicket.getEventInfo();
        this.userName = user.getProfile().getName();
    }

}
