package band.gosrock.api.v2.ticket.dto.response

import band.gosrock.api.v2.ticket.dto.V2TicketOptionType
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketSaleState
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** T-1 관리용 티켓 (재고 항상 공개) */
data class V2TicketItemManageResponse(
    val ticketItemId: Long,
    @field:Schema(description = "DUDOONG / FREE / PRICE(기존 PG 티켓, v2 수정 불가)")
    val payType: V2TicketPayType?,
    val name: String?,
    val description: String?,
    @field:Schema(description = "가격(원)")
    val price: Long,
    @field:Schema(description = "판매 수량. null 이면 무제한")
    val supplyCount: Long?,
    @field:Schema(description = "잔여 수량 (재고 공개 여부와 무관). 무제한이면 null")
    val remaining: Long?,
    @field:Schema(description = "판매(재고 감소)된 수량")
    val soldCount: Long,
    val approvalRequired: Boolean,
    val isQuantityPublic: Boolean,
    @field:Schema(description = "1인 구매 매수 제한. null 이면 제한 없음")
    val purchaseLimit: Long?,
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "판매 시작. null 이면 등록 즉시")
    @field:DateFormat
    val saleStartAt: LocalDateTime?,
    @field:Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "판매 종료. null 이면 공연 시작까지")
    @field:DateFormat
    val saleEndAt: LocalDateTime?,
    @field:Schema(description = "BEFORE_SALE(재고 감소 없음) / SOLD(재고 감소) / SUSPENDED(판매 중단)")
    val saleState: V2TicketSaleState,
    @field:Schema(description = "재고 감소 여부. true 면 삭제 불가 (중단 상태여도 유지)")
    val isSold: Boolean,
    @field:Schema(description = "승인 대기 주문 존재 여부. isSold 또는 이 값이 true 면 잠김: 설명·판매기간·재고공개·매수제한·수량 증가만 수정, 옵션 변경·옵션 추가금 변경 불가")
    val hasPendingOrders: Boolean,
    @field:Schema(description = "승인 대기 주문 수량 합 (#726). remaining(재고)에서 빠지지 않은 값 — 사용자 앱 잔여(P-5) = remaining - 이 값")
    val pendingApproveCount: Long,
    @field:Schema(description = "지금 사용자가 살 수 있는지: 판매 중 + 판매 기간 + 공연 등록 + 공연 시작 전 + 재고 > 0")
    val isPurchasable: Boolean,
    @field:Schema(description = "입금 계좌 (DUDOONG 만)")
    val account: V2TicketAccountResponse?,
    @field:Schema(description = "붙은 옵션 (id 순)")
    val options: List<V2AppliedOptionResponse>,
)

/** 입금 계좌 (#755: 사용자 앱 O-0·O-3 계좌와 같은 이름) */
data class V2TicketAccountResponse(
    val bankName: String?,
    val accountHolder: String?,
    val accountNumber: String?,
)

data class V2AppliedOptionResponse(
    val optionId: Long,
    val name: String?,
    val type: V2TicketOptionType?,
    @field:Schema(description = "'네' 선택 시 추가 금액. YES_NO 가 아니면 null")
    val yesAdditionalPrice: Long?,
)

/** O-1 공연 옵션 풀 */
data class V2TicketOptionResponse(
    val optionId: Long,
    val name: String?,
    val description: String?,
    val type: V2TicketOptionType?,
    @field:Schema(description = "'네' 선택 시 추가 금액. YES_NO 가 아니면 null")
    val yesAdditionalPrice: Long?,
    @field:Schema(description = "이 옵션이 붙은 유효 티켓 id")
    val appliedTicketItemIds: List<Long>,
    @field:Schema(description = "잠긴 티켓(판매됨 또는 승인 대기 주문 있음)에 붙어 있으면 true: 이름·설명만 수정, 삭제·떼기 불가")
    val isLocked: Boolean,
)
