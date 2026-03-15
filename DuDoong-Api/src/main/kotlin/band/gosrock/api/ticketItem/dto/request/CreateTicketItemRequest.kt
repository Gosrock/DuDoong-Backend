package band.gosrock.api.ticketItem.dto.request

import band.gosrock.common.annotation.Enum
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

data class CreateTicketItemRequest(
    @field:Schema(nullable = false, defaultValue = "두둥티켓")
    @field:Enum(message = "두둥티켓, 무료티켓, 유료티켓만 허용됩니다")
    val payType: TicketPayType? = null,

    @field:NotEmpty(message = "티켓상품 이름을 입력해주세요")
    @field:Schema(nullable = false, example = "일반 티켓")
    val name: String? = null,

    @field:Schema(nullable = true, example = "일반 입장 티켓입니다.")
    val description: String? = null,

    @field:Schema(nullable = true, example = "신한은행")
    val bankName: String? = null,

    @field:Schema(nullable = true, example = "110-123-1234567")
    val accountNumber: String? = null,

    @field:Schema(nullable = true, example = "김원진")
    val accountHolder: String? = null,

    @field:NotNull
    @field:Schema(defaultValue = "0", nullable = false, example = "4000")
    val price: Long? = null,

    @field:Positive
    @field:Schema(nullable = false, example = "100")
    val supplyCount: Long? = null,

    @field:Schema(nullable = false, defaultValue = "승인")
    @field:Enum(message = "선착순, 승인만 허용됩니다")
    val approveType: TicketType? = null,

    @field:NotNull
    @field:Schema(nullable = false, example = "true")
    val isQuantityPublic: Boolean? = null,

    @field:NotNull
    @field:Schema(nullable = false, example = "1")
    val purchaseLimit: Long? = null,
)
