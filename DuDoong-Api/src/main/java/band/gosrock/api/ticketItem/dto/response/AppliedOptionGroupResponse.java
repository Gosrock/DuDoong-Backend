package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppliedOptionGroupResponse {

    @Schema(description = "TicketItemResponse")
    @JsonUnwrapped
    private final TicketItemResponse ticketItemResponse;

    @Schema(description = "적용된 옵션그룹 리스트")
    private final List<OptionGroupResponse> optionGroups;

    public static AppliedOptionGroupResponse from(
            TicketItem ticketItem, List<OptionGroupResponse> optionGroup) {

        return AppliedOptionGroupResponse.builder()
                .ticketItemResponse(TicketItemResponse.from(ticketItem, Boolean.TRUE))
                .optionGroups(optionGroup)
                .build();
    }
}
