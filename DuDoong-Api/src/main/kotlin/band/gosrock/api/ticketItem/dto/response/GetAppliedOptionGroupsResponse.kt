package band.gosrock.api.ticketItem.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class GetAppliedOptionGroupsResponse(
    @field:Schema(description = "적용 현황 리스트")
    val appliedOptionGroups: List<AppliedOptionGroupResponse>,
) {
    companion object {
        @JvmStatic
        fun from(appliedOptionGroups: List<AppliedOptionGroupResponse>): GetAppliedOptionGroupsResponse =
            GetAppliedOptionGroupsResponse(appliedOptionGroups = appliedOptionGroups)
    }
}
