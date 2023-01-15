package band.gosrock.api.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveIssuedTicketDTO {

    private final IssuedTicketInfoVo issuedTicketInfo;

    private final UserInfoVo userInfo;

    public static RetrieveIssuedTicketDTO of(IssuedTicket issuedTicket, User user) {
        return RetrieveIssuedTicketDTO.builder()
                .issuedTicketInfo(issuedTicket.toIssuedTicketInfoVo())
                .userInfo(user.toUserInfoVo())
                .build();
    }
}
