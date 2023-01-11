package band.gosrock.domain.domains.issuedTicket.dto.request;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateIssuedTicketRequest {

    /*
    발급 티켓의 이벤트 id
     */
    private final Event event;

    /*
    발급 티켓의 orderline id
     */
    private Long orderLineId;

    /*
    티켓 발급한 유저 id
     */
    private final User user;

    /*
    발급 티켓 가격
     */
    private final Long price;

    /*
    발급 티켓의 티켓 itemId (발급 티켓의 종류)
     */
    private final TicketItem ticketItem;

    /*
    발급 티켓에 걸려오는 옵션들 CartOptionAnswer를 List로 받습니다.
     */
    private final List<OrderOptionAnswer> optionAnswers;
}
