package band.gosrock.api.order.model.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderLineTicketResponse {
    private final String ticketName;
    private final String orderStatus;
    private final String orderNo;
    private final String ticketNos;
    private final LocalDateTime paymentAt;
    private final String userName;
}
