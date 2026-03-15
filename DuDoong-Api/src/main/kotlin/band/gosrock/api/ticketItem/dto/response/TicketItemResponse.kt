package band.gosrock.api.ticketItem.dto.response

import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import io.swagger.v3.oas.annotations.media.Schema

data class TicketItemResponse(
    @field:Schema(description = "티켓상품 id")
    val ticketItemId: Long?,

    @field:Schema(description = "티켓 지불 타입")
    val payType: TicketPayType?,

    @field:Schema(description = "이름")
    val ticketName: String?,

    @field:Schema(description = "설명")
    val description: String?,

    @field:Schema(description = "가격")
    val price: Money?,

    @field:Schema(description = "티켓 승인 타입")
    val approveType: TicketType?,

    @field:Schema(description = "1인당 구매 제한 매수")
    val purchaseLimit: Long?,

    @field:Schema(description = "공급량")
    val supplyCount: Long?,

    @field:Schema(description = "재고")
    val quantity: Long?,

    @field:Schema(description = "재고공개 여부")
    val isQuantityPublic: Boolean?,

    @field:Schema(description = "계좌 정보")
    val accountInfo: AccountInfoVo?,

    @field:Schema(description = "재고가 감소한 티켓인지 리턴")
    val isSold: Boolean?,

    @field:Schema(description = "재고가 남아있는지 리턴")
    val isQuantityLeft: Boolean?,
) {
    companion object {
        @JvmStatic
        fun from(ticketItem: TicketItem, isAdmin: Boolean): TicketItemResponse = TicketItemResponse(
            ticketItemId = ticketItem.id,
            payType = ticketItem.payType,
            ticketName = ticketItem.name,
            description = ticketItem.description,
            price = ticketItem.price,
            approveType = ticketItem.type,
            purchaseLimit = ticketItem.purchaseLimit,
            supplyCount = ticketItem.supplyCount,
            quantity = if (isAdmin || ticketItem.isQuantityPublic == true) ticketItem.quantity else null,
            isQuantityPublic = ticketItem.isQuantityPublic,
            accountInfo = ticketItem.accountInfo,
            isSold = ticketItem.isSold(),
            isQuantityLeft = ticketItem.isQuantityLeft(),
        )
    }
}
