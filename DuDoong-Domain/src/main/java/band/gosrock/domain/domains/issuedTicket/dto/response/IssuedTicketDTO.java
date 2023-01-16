package band.gosrock.domain.domains.issuedTicket.dto.response;


import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssuedTicketDTO {

    private final IssuedTicketInfoVo issuedTicketInfo;

    private final EventInfoVo eventInfo;
}
