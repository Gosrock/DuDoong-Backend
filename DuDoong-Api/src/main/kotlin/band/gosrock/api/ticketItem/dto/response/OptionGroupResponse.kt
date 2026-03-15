package band.gosrock.api.ticketItem.dto.response

import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import io.swagger.v3.oas.annotations.media.Schema

data class OptionGroupResponse(
    @field:Schema(description = "옵션그룹 id")
    val optionGroupId: Long?,

    @field:Schema(description = "옵션그룹 타입")
    val type: OptionGroupType?,

    @field:Schema(description = "이름")
    val name: String?,

    @field:Schema(description = "설명")
    val description: String?,

    val options: List<OptionResponse>,
) {
    companion object {
        @JvmStatic
        fun from(optionGroup: OptionGroup): OptionGroupResponse = OptionGroupResponse(
            optionGroupId = optionGroup.id,
            type = optionGroup.type,
            name = optionGroup.name,
            description = optionGroup.description,
            options = optionGroup.options.map { OptionResponse.from(it) },
        )
    }
}
