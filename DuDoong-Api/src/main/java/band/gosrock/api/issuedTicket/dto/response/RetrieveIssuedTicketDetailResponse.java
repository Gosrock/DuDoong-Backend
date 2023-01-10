package band.gosrock.api.issuedTicket.dto.response;

import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RetrieveIssuedTicketDetailResponse {

    /*
    이벤트 정보 VO
     */
    private final EventInfoVo eventInfo;

    /*
    발급 티켓 정보 VO
     */
    private final IssuedTicketInfoVo issuedTicketInfo;

    /*
    발급 유저 이름
     */
    private final String userName;

    public RetrieveIssuedTicketDetailResponse(IssuedTicket issuedTicket, User user) {
        this.eventInfo = new EventInfoVo(issuedTicket.getEvent());
        this.issuedTicketInfo = new IssuedTicketInfoVo(issuedTicket);
        // Todo : user 풀어 헤칠지 말지 고민 중
        this.userName = user.getProfile().getName();
    }

}
