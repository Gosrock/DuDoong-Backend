package band.gosrock.api.cart.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Min

data class AddCartLineDto(
    @Schema(description = "주문할 아이템 아이디", defaultValue = "1")
    val itemId: Long,

    @Schema(description = "상품 수량", defaultValue = "1")
    @field:Min(1)
    val quantity: Long,

    @Schema(description = "상품 관련 옵션에 대한 답변")
    val options: List<AddCartOptionAnswerDto>,
)
