package band.gosrock.api.ticketItem.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class GetEventOptionsResponse(
    @field:Schema(description = "옵션그룹 리스트")
    val optionGroups: List<OptionGroupResponse>,
) {
    companion object {
        @JvmStatic
        fun from(optionGroups: List<OptionGroupResponse>): GetEventOptionsResponse =
            GetEventOptionsResponse(optionGroups = optionGroups)
    }
}
