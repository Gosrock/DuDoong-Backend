package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppliedOptionGroupResponse {

    @Schema(description = "티켓상품 id")
    private final Long ticketItemId;

    @Schema(description = "이름")
    private final String ticketName;

    @Schema(description = "적용된 옵션그룹 리스트")
    private final List<OptionGroupResponse> optionGroups;

    public static AppliedOptionGroupResponse from(
            TicketItem ticketItem, List<OptionGroupResponse> optionGroup) {

        return AppliedOptionGroupResponse.builder()
                .ticketItemId(ticketItem.getId())
                .ticketName(ticketItem.getName())
                .optionGroups(optionGroup)
                .build();
    }
}
