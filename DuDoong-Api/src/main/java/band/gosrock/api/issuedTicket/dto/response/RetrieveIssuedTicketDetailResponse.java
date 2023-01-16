package band.gosrock.api.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
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

    public static RetrieveIssuedTicketDetailResponse of(IssuedTicket issuedTicket) {
        return RetrieveIssuedTicketDetailResponse.builder()
                .issuedTicketInfo(issuedTicket.toIssuedTicketInfoVo())
                .eventInfo(issuedTicket.getEvent().toEventInfoVo())
                .userName(issuedTicket.getUser().getProfile().getName())
                .build();
    }
}
