package band.gosrock.api.issuedTicket.dto.request;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostIssuedTicketRequest {

    /*
    발급 티켓의 이벤트 id
     */
    private Long eventId;

    /*
    발급 티켓의 orderline id
     */
    private Long orderLineId;

    /*
    티켓 발급한 유저 id
     */
    private Long userId;

    /*
    발급 티켓의 티켓 itemId (발급 티켓의 종류)
     */
    private Long ticketItemId;

    /*
    발급 티켓에 걸려오는 옵션들 (Collection Map으로 처리)
     */
    private Map<Long, Long> option;
}
