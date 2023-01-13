package band.gosrock.api.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Getter;

@Getter
public class RetrieveIssuedTicketDTO {

    private final IssuedTicketInfoVo issuedTicketInfo;

    private final UserInfoVo userInfo;

    public RetrieveIssuedTicketDTO(IssuedTicket issuedTicket, User user) {
        this.issuedTicketInfo = new IssuedTicketInfoVo(issuedTicket);
        this.userInfo = new UserInfoVo(user);
    }
}
