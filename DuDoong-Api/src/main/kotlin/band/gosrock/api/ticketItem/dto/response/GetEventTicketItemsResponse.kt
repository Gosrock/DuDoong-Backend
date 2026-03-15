package band.gosrock.api.ticketItem.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class GetEventTicketItemsResponse(
    @field:Schema(description = "티켓상품 리스트")
    val ticketItems: List<TicketItemResponse>,
) {
    companion object {
        @JvmStatic
        fun from(ticketItems: List<TicketItemResponse>): GetEventTicketItemsResponse =
            GetEventTicketItemsResponse(ticketItems = ticketItems)
    }
}
