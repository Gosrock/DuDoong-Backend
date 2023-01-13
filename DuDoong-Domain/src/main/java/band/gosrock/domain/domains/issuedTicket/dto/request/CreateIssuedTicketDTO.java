package band.gosrock.domain.domains.issuedTicket.dto.request;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateIssuedTicketDTO {

    private final Order order;

    private final OrderLineItem orderLineItem;

    private final User user;
}
