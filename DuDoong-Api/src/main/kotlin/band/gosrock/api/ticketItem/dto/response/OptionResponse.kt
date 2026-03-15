package band.gosrock.api.ticketItem.dto.response

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.Option
import io.swagger.v3.oas.annotations.media.Schema

data class OptionResponse(
    @field:Schema(description = "옵션 id")
    val optionId: Long?,

    @field:Schema(description = "응답")
    val answer: String?,

    @field:Schema(description = "추가 금액")
    val additionalPrice: Money?,
) {
    companion object {
        @JvmStatic
        fun from(option: Option): OptionResponse = OptionResponse(
            optionId = option.id,
            answer = option.answer,
            additionalPrice = option.additionalPrice,
        )
    }
}
