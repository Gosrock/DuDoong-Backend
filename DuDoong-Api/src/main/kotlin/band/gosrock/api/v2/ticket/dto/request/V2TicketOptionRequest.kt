package band.gosrock.api.v2.ticket.dto.request

import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketOptionDomainService
import jakarta.validation.constraints.Max
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

/** O-2 옵션 생성 */
data class V2CreateTicketOptionRequest(
    @field:Schema(description = "옵션 이름 (1~${V2TicketOptionDomainService.NAME_MAX_LENGTH}자)", example = "뒷풀이 참석")
    @field:NotBlank
    @field:Size(max = V2TicketOptionDomainService.NAME_MAX_LENGTH)
    val name: String?,

    @field:Schema(description = "옵션 설명", example = "공연 후 뒷풀이에 참석하나요?")
    @field:NotBlank
    @field:Size(max = V2TicketOptionDomainService.DESCRIPTION_MAX_LENGTH)
    val description: String?,

    @field:Schema(description = "응답 형식 SUBJECTIVE / YES_NO (MULTIPLE_CHOICE 는 400). 만든 뒤 바꿀 수 없음", example = "YES_NO")
    @field:NotNull
    val type: V2TicketOptionType?,

    @field:Schema(description = "'네' 선택 시 추가 금액 (YES_NO 만, 0 ~ ${V2TicketItemDomainService.MAX_PRICE}, 없으면 0)", example = "10000")
    @field:PositiveOrZero
    @field:Max(V2TicketItemDomainService.MAX_PRICE)
    val yesAdditionalPrice: Long? = null,
)

/** O-3 옵션 부분 수정. null 은 변경 안 함. 응답 형식은 바꿀 수 없다 */
data class V2UpdateTicketOptionRequest(
    @field:Schema(description = "옵션 이름 (1~${V2TicketOptionDomainService.NAME_MAX_LENGTH}자)")
    @field:Size(min = 1, max = V2TicketOptionDomainService.NAME_MAX_LENGTH)
    @field:Pattern(regexp = ".*\\S.*", message = "공백만으로는 이름을 지을 수 없습니다")
    val name: String? = null,

    @field:Schema(description = "옵션 설명")
    @field:Size(min = 1, max = V2TicketOptionDomainService.DESCRIPTION_MAX_LENGTH)
    @field:Pattern(regexp = "(?s).*\\S.*", message = "설명을 입력해주세요")
    val description: String? = null,

    @field:Schema(description = "'네' 선택 시 추가 금액 (YES_NO 만). 잠긴 티켓(판매됨·승인 대기)에 붙은 옵션은 바꿀 수 없음")
    @field:PositiveOrZero
    @field:Max(V2TicketItemDomainService.MAX_PRICE)
    val yesAdditionalPrice: Long? = null,
)
