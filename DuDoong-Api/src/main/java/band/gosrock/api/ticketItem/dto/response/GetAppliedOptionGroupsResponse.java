package band.gosrock.api.ticketItem.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetAppliedOptionGroupsResponse {

    @Schema(description = "적용 현황 리스트")
    private List<AppliedOptionGroupResponse> appliedOptionGroups;

    public static GetAppliedOptionGroupsResponse from(
            List<AppliedOptionGroupResponse> appliedOptionGroups) {

        return GetAppliedOptionGroupsResponse.builder()
                .appliedOptionGroups(appliedOptionGroups)
                .build();
    }
}
