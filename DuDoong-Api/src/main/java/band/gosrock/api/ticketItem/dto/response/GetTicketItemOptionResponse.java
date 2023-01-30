package band.gosrock.api.ticketItem.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetTicketItemOptionResponse {

    @Schema(description = "옵션그룹 리스트")
    private List<OptionGroupResponse> optionGroups;

    public static GetTicketItemOptionResponse from(List<OptionGroupResponse> optionGroups) {

        return GetTicketItemOptionResponse.builder().optionGroups(optionGroups).build();
    }
}
