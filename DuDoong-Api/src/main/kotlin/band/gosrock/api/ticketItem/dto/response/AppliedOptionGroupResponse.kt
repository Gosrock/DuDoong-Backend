package band.gosrock.api.ticketItem.dto.response

import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema

data class AppliedOptionGroupResponse(
    @field:Schema(description = "TicketItemResponse")
    @field:JsonUnwrapped
    val ticketItemResponse: TicketItemResponse,

    @field:Schema(description = "적용된 옵션그룹 리스트")
    val optionGroups: List<OptionGroupResponse>,
) {
    companion object {
        @JvmStatic
        fun from(ticketItem: TicketItem, optionGroups: List<OptionGroupResponse>): AppliedOptionGroupResponse =
            AppliedOptionGroupResponse(
                ticketItemResponse = TicketItemResponse.from(ticketItem, true),
                optionGroups = optionGroups,
            )
    }
}
