package band.gosrock.api.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo;
import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetrieveIssuedTicketDTO {

    private final IssuedTicketInfoVo issuedTicketInfo;

//    private final UserInfoVo userInfo;

    private final List<IssuedTicketOptionAnswerVo> issuedTicketOptionAnswers;

    public static RetrieveIssuedTicketDTO of(IssuedTicket issuedTicket) {
        return RetrieveIssuedTicketDTO.builder()
                .issuedTicketInfo(issuedTicket.toIssuedTicketInfoVo())
                .issuedTicketOptionAnswers(
                        issuedTicket.getIssuedTicketOptionAnswers().stream()
                                .map(IssuedTicketOptionAnswer::toIssuedTicketOptionAnswerVo)
                                .toList())
                .build();
    }
}
