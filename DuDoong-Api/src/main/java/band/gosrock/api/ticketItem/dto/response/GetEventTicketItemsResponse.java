package band.gosrock.api.ticketItem.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetEventTicketItemsResponse {

    @Schema(description = "티켓상품 리스트")
    private List<TicketItemResponse> ticketItems;

    public static GetEventTicketItemsResponse from(List<TicketItemResponse> ticketItems) {

        return GetEventTicketItemsResponse.builder().ticketItems(ticketItems).build();
    }
}
