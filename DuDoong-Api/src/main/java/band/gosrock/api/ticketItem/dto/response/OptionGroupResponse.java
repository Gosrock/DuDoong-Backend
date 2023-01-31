package band.gosrock.api.ticketItem.dto.response;


import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OptionGroupResponse {
    @Schema(description = "옵션그룹 id")
    private final Long optionGroupId;

    @Schema(description = "옵션그룹 타입")
    private final OptionGroupType type;

    @Schema(description = "이름")
    private final String name;

    @Schema(description = "설명")
    private final String description;

    private final List<OptionResponse> options;

    public static OptionGroupResponse from(OptionGroup optionGroup) {

        return OptionGroupResponse.builder()
                .optionGroupId(optionGroup.getId())
                .type(optionGroup.getType())
                .name(optionGroup.getName())
                .description(optionGroup.getDescription())
                .options(optionGroup.getOptions().stream().map(OptionResponse::from).toList())
                .build();
    }
}
