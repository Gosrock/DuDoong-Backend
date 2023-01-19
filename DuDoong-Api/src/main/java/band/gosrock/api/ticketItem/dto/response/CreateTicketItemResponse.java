package band.gosrock.api.ticketItem.dto.response;

import band.gosrock.api.order.model.dto.response.OrderPaymentResponse;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.PaymentInfo;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class CreateTicketItemResponse {
    @Schema(description = "ticketItemId")
    private final Long ticketItemId;

    public static CreateTicketItemResponse from(TicketItem ticketItem) {

        return CreateTicketItemResponse.builder()
                .ticketItemId(ticketItem.getId())
                .build();
    }
}
