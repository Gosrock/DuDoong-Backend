package band.gosrock.api.ticketItem.dto.request

import band.gosrock.common.annotation.Enum
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class CreateTicketOptionRequest(
    @field:Schema(nullable = false, defaultValue = "Y/N")
    @field:Enum(message = "Y/N, 주관식, 객관식만 허용됩니다")
    val type: OptionGroupType? = null,

    @field:NotEmpty(message = "옵션그룹 이름을 입력해주세요")
    @field:Schema(nullable = false, example = "뒷풀이 참여 여부")
    val name: String? = null,

    @field:Schema(nullable = true, example = "공연이 끝난 후 오케이포차에서 진행하는 뒷풀이에 참여할 것인가요?")
    val description: String? = null,

    @field:NotNull
    @field:Schema(nullable = false, example = "10000")
    val additionalPrice: Long? = null,
)
