package band.gosrock.admin.model.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminChangeNameRequest(
    @field:NotBlank(message = "이름을 입력해주세요.")
    @field:Size(min = 2, max = 7, message = "이름은 2~7자여야 합니다.")
    val name: String,
)
