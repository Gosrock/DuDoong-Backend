package band.gosrock.api.ticketItem.dto.response

import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import io.swagger.v3.oas.annotations.media.Schema

data class ApplyTicketOptionResponse(
    @field:Schema(description = "티켓상품 id")
    val ticketItemId: Long?,

    @field:Schema(description = "옵션그룹 id 리스트")
    val optionGroupIds: List<Long>,
) {
    companion object {
        @JvmStatic
        fun from(ticketItem: TicketItem): ApplyTicketOptionResponse = ApplyTicketOptionResponse(
            ticketItemId = ticketItem.id,
            optionGroupIds = ticketItem.getOptionGroupIds(),
        )
    }
}
