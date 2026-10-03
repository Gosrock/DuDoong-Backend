package band.gosrock.api.v2.ticket.dto.request

import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemForm
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 티켓 생성(T-2) / 수정(T-3) 폼. 수정도 폼 전체를 보낸다 (null 은 '변경 안 함' 이 아니라 무제한·등록 즉시 등 값 자체).
 * 판매된 티켓은 잠긴 필드(종류·이름·가격·계좌·승인 여부)를 현재 값 그대로 보내야 하고, 수량은 늘리기만 할 수 있다.
 */
data class V2TicketItemRequest(
    @field:Schema(description = "결제 방식. DUDOONG(계좌송금, 승인 필수) / FREE. PRICE 는 400", example = "DUDOONG")
    @field:NotNull
    val payType: V2TicketPayType?,

    @field:Schema(description = "티켓 이름 (1~${V2TicketItemDomainService.NAME_MAX_LENGTH}자)", example = "일반 티켓")
    @field:NotBlank
    @field:Size(max = V2TicketItemDomainService.NAME_MAX_LENGTH)
    val name: String?,

    @field:Schema(description = "티켓 설명 (~${V2TicketItemDomainService.DESCRIPTION_MAX_LENGTH}자)", example = "일반 입장 티켓")
    @field:Size(max = V2TicketItemDomainService.DESCRIPTION_MAX_LENGTH)
    val description: String? = null,

    @field:Schema(description = "가격(원). DUDOONG 은 1 이상, FREE 는 0", example = "6000")
    @field:NotNull
    @field:PositiveOrZero
    val price: Long?,

    @field:Schema(description = "판매 수량. null 이면 무제한", example = "100")
    @field:Min(1)
    @field:Max(V2TicketItemDomainService.MAX_SUPPLY_COUNT)
    val supplyCount: Long? = null,

    @field:Schema(description = "입금 계좌 (DUDOONG 필수, FREE 는 무시)")
    @field:Valid
    val account: V2TicketAccountRequest? = null,

    @field:Schema(description = "관리자 승인 여부. DUDOONG 은 항상 true 로 저장", example = "true")
    @field:NotNull
    val approvalRequired: Boolean?,

    @field:Schema(description = "재고(잔여 매수) 공개 여부. 무제한 티켓은 false 로 저장", example = "true")
    @field:NotNull
    val isQuantityPublic: Boolean?,

    @field:Schema(description = "1인 구매 매수 제한. null 이면 제한 없음", example = "4")
    @field:Min(1)
    @field:Max(V2TicketItemDomainService.MAX_SUPPLY_COUNT)
    val purchaseLimit: Long? = null,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "판매 시작. null 이면 등록 즉시")
    @field:DateFormat
    val saleStartAt: LocalDateTime? = null,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "판매 종료 (공연 시작 이하). null 이면 공연 시작까지")
    @field:DateFormat
    val saleEndAt: LocalDateTime? = null,
) {
    fun toForm(): V2TicketItemForm = V2TicketItemForm(
        payType = payType!!.domain,
        name = name!!,
        description = description,
        price = price!!,
        supplyCount = supplyCount,
        account = account?.let { AccountInfoVo(bankName = it.bank, accountNumber = it.number, accountHolder = it.holder) },
        approvalRequired = approvalRequired!!,
        isQuantityPublic = isQuantityPublic!!,
        purchaseLimit = purchaseLimit,
        saleStartAt = saleStartAt,
        saleEndAt = saleEndAt,
    )
}

data class V2TicketAccountRequest(
    @field:Schema(description = "은행명", example = "신한은행")
    @field:NotBlank
    @field:Size(max = 30)
    val bank: String?,

    @field:Schema(description = "예금주", example = "고스락")
    @field:NotBlank
    @field:Size(max = 30)
    val holder: String?,

    @field:Schema(description = "계좌번호", example = "110-123-456789")
    @field:NotBlank
    @field:Size(max = 50)
    val number: String?,
)

/** O-5 티켓에 붙은 옵션 전체 지정 */
data class V2ReplaceTicketOptionsRequest(
    @field:Schema(description = "옵션 id 목록 (빈 배열이면 모두 떼기, 중복은 하나로)")
    @field:NotNull
    @field:Size(max = V2TicketItemDomainService.MAX_OPTION_COUNT)
    val optionIds: List<Long>?,
)
