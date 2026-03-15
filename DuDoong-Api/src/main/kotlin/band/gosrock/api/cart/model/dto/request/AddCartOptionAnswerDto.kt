package band.gosrock.api.cart.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class AddCartOptionAnswerDto(
    @Schema(description = "옵션 아이디")
    @field:NotNull
    val optionId: Long,

    @Schema(description = "옵션 그룹에 대한 응답/ T/F면 예,아니오 ,서술형이면 서술응답", defaultValue = "예")
    @field:NotBlank
    val answer: String,
)
