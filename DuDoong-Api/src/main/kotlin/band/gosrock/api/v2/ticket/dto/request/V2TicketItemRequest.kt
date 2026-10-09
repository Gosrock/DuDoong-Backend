package band.gosrock.api.v2.ticket.dto.request

import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketClearableField
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemForm
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemPatch
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/** 티켓 생성(T-2) 폼. 선택 필드의 null 은 '값 없음'(무제한·제한 없음·등록 즉시·공연 시작까지). 수정(T-3)은 [V2UpdateTicketItemRequest] */
data class V2TicketItemRequest(
    @field:Schema(description = "결제 방식. DUDOONG(계좌송금, 승인 필수) / FREE. PRICE 는 400", example = "DUDOONG")
    @field:NotNull
    val payType: V2TicketPayType?,

    @field:Schema(description = "티켓 이름 (1~${V2TicketItemDomainService.NAME_MAX_LENGTH}자). 길이는 값이 바뀔 때만 검증 (v1 의 긴 이름은 그대로 재전송 가능)", example = "일반 티켓")
    @field:NotBlank
    @field:Size(max = RAW_TEXT_MAX)
    val name: String?,

    @field:Schema(description = "티켓 설명 (~${V2TicketItemDomainService.DESCRIPTION_MAX_LENGTH}자). 길이는 값이 바뀔 때만 검증", example = "일반 입장 티켓")
    @field:Size(max = RAW_TEXT_MAX)
    val description: String? = null,

    @field:Schema(description = "가격(원). DUDOONG 은 1 ~ ${V2TicketItemDomainService.MAX_PRICE}, FREE 는 0", example = "6000")
    @field:NotNull
    @field:PositiveOrZero
    @field:Max(V2TicketItemDomainService.MAX_PRICE)
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

    @field:Schema(description = "재고(잔여 매수) 공개 여부. 무제한(supplyCount=null)이면 true 불가 (400)", example = "true")
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
        account = account?.toVo(),
        approvalRequired = approvalRequired!!,
        isQuantityPublic = isQuantityPublic!!,
        purchaseLimit = purchaseLimit,
        saleStartAt = saleStartAt,
        saleEndAt = saleEndAt,
    )

    companion object {
        /** 길이 규칙은 도메인에서 (값이 바뀔 때만). 여기서는 과도한 입력만 막는다 (컬럼 varchar(255)) */
        private const val RAW_TEXT_MAX = 255
    }
}

/**
 * T-3 티켓 부분 수정 (#755). **null(키 없음) = 변경 안 함** — 다른 v2 PATCH 와 같다. 바꿀 필드만 보내도 되고, 지금처럼 전체 값을 보내도 결과가 같다.
 * '값 없음'(무제한·제한 없음·등록 즉시·공연 시작까지·설명 없음)으로 바꾸려면 [clear] 에 필드 이름을 넣는다 (null 을 보내는 것으로는 바뀌지 않음).
 * 판매된 티켓의 잠긴 필드 규칙(DEC-006·DEC-020)은 그대로: 종류·이름·가격·계좌·승인 여부는 현재 값과 같아야 하고 수량은 늘리기만.
 */
data class V2UpdateTicketItemRequest(
    @field:Schema(description = "결제 방식. DUDOONG / FREE (PRICE 는 400). null 이면 그대로", example = "DUDOONG")
    val payType: V2TicketPayType? = null,

    @field:Schema(description = "티켓 이름 (1~${V2TicketItemDomainService.NAME_MAX_LENGTH}자, 길이는 값이 바뀔 때만 검증). null 이면 그대로", example = "일반 티켓")
    @field:Size(max = RAW_TEXT_MAX)
    val name: String? = null,

    @field:Schema(description = "티켓 설명 (~${V2TicketItemDomainService.DESCRIPTION_MAX_LENGTH}자). null 이면 그대로, 비우려면 clear 에 DESCRIPTION", example = "일반 입장 티켓")
    @field:Size(max = RAW_TEXT_MAX)
    val description: String? = null,

    @field:Schema(description = "가격(원). null 이면 그대로", example = "6000")
    @field:PositiveOrZero
    @field:Max(V2TicketItemDomainService.MAX_PRICE)
    val price: Long? = null,

    @field:Schema(description = "판매 수량. null 이면 그대로, 무제한은 clear 에 SUPPLY_COUNT", example = "100")
    @field:Min(1)
    @field:Max(V2TicketItemDomainService.MAX_SUPPLY_COUNT)
    val supplyCount: Long? = null,

    @field:Schema(description = "입금 계좌 (DUDOONG). null 이면 그대로, 보내면 세 값 모두 교체")
    @field:Valid
    val account: V2TicketAccountRequest? = null,

    @field:Schema(description = "관리자 승인 여부. null 이면 그대로 (DUDOONG 은 항상 true 로 저장)")
    val approvalRequired: Boolean? = null,

    @field:Schema(description = "재고 공개 여부. null 이면 그대로 (무제한이면 true 불가)")
    val isQuantityPublic: Boolean? = null,

    @field:Schema(description = "1인 구매 매수 제한. null 이면 그대로, 제한 없음은 clear 에 PURCHASE_LIMIT", example = "4")
    @field:Min(1)
    @field:Max(V2TicketItemDomainService.MAX_SUPPLY_COUNT)
    val purchaseLimit: Long? = null,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "판매 시작. null 이면 그대로, 등록 즉시는 clear 에 SALE_START_AT")
    @field:DateFormat
    val saleStartAt: LocalDateTime? = null,

    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "판매 종료. null 이면 그대로, 공연 시작까지는 clear 에 SALE_END_AT")
    @field:DateFormat
    val saleEndAt: LocalDateTime? = null,

    @field:Schema(
        description = "'값 없음'으로 바꿀 필드: SUPPLY_COUNT(무제한) / PURCHASE_LIMIT(제한 없음) / SALE_START_AT(등록 즉시) / SALE_END_AT(공연 시작까지) / DESCRIPTION(설명 없음). " +
            "같은 필드에 값도 보내면 400 (Ticket_Item_400_15)",
        example = "[\"SUPPLY_COUNT\"]",
    )
    @field:Size(max = 5)
    val clear: Set<V2TicketClearableField>? = null,
) {
    fun toPatch(): V2TicketItemPatch = V2TicketItemPatch(
        payType = payType?.domain,
        name = name,
        description = description,
        price = price,
        supplyCount = supplyCount,
        account = account?.toVo(),
        approvalRequired = approvalRequired,
        isQuantityPublic = isQuantityPublic,
        purchaseLimit = purchaseLimit,
        saleStartAt = saleStartAt,
        saleEndAt = saleEndAt,
        clear = clear.orEmpty(),
    )

    companion object {
        private const val RAW_TEXT_MAX = 255
    }
}

/** 입금 계좌 (#755: 사용자 앱·환불 계좌와 같은 이름 bankName / accountHolder / accountNumber) */
data class V2TicketAccountRequest(
    @field:Schema(description = "은행명", example = "신한은행")
    @field:NotBlank
    @field:Size(max = 30)
    val bankName: String?,

    @field:Schema(description = "예금주", example = "고스락")
    @field:NotBlank
    @field:Size(max = 30)
    val accountHolder: String?,

    @field:Schema(description = "계좌번호", example = "110-123-456789")
    @field:NotBlank
    @field:Size(max = 50)
    val accountNumber: String?,
) {
    fun toVo(): AccountInfoVo = AccountInfoVo(bankName = bankName, accountNumber = accountNumber, accountHolder = accountHolder)
}

/** O-5 티켓에 붙은 옵션 전체 지정 */
data class V2ReplaceTicketOptionsRequest(
    @field:Schema(description = "옵션 id 목록 (빈 배열이면 모두 떼기, 중복은 하나로)")
    @field:NotNull
    @field:Size(max = V2TicketItemDomainService.MAX_OPTION_COUNT)
    val optionIds: List<Long>?,
)
