package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplyTicketOptionResponse {

    @Schema(description = "티켓상품 id")
    private final Long ticketItemId;

    @Schema(description = "옵션그룹 id 리스트")
    private final List<Long> optionGroupIds;

    public static ApplyTicketOptionResponse from(TicketItem ticketItem) {
        return ApplyTicketOptionResponse.builder()
                .ticketItemId(ticketItem.getId())
                .optionGroupIds(ticketItem.getOptionGroupIds())
                .build();
    }
}
