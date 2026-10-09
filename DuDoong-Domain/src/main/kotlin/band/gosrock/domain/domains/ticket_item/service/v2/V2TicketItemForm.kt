package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import java.time.LocalDateTime

/**
 * v2 티켓 생성(T-2) 입력, 그리고 수정(T-3) 때 현재 값에 [V2TicketItemPatch] 를 덮어쓴 결과.
 * @property supplyCount null 이면 무제한 ([band.gosrock.domain.domains.ticket_item.domain.TicketItem.UNLIMITED_SUPPLY_COUNT] 로 저장)
 * @property purchaseLimit null 이면 1인 매수 제한 없음 ([band.gosrock.domain.domains.ticket_item.domain.TicketItem.NO_PURCHASE_LIMIT] 로 저장)
 * @property approvalRequired 두둥티켓은 항상 true 로 저장
 * @property isQuantityPublic 무제한(supplyCount=null)이면 true 불가 (400)
 * @property saleStartAt null 이면 등록 즉시, saleEndAt null 이면 공연 시작까지
 */
data class V2TicketItemForm(
    val payType: TicketPayType,
    val name: String,
    val description: String?,
    val price: Long,
    val supplyCount: Long?,
    val account: AccountInfoVo?,
    val approvalRequired: Boolean,
    val isQuantityPublic: Boolean,
    val purchaseLimit: Long?,
    val saleStartAt: LocalDateTime?,
    val saleEndAt: LocalDateTime?,
)

/**
 * T-3 부분 수정 (#755). null 은 '변경 안 함'(다른 v2 PATCH 와 같음). '값 없음'(무제한·제한 없음·등록 즉시·공연 시작까지·설명 없음)으로
 * 바꾸려면 [clear] 에 그 필드를 넣는다. 같은 필드를 값과 [clear] 에 함께 주면 400 (Ticket_Item_400_15).
 * 현재 값에 덮어써 [V2TicketItemForm] 을 만든 뒤 수정 규칙(잠긴 필드 DEC-006·DEC-020)은 폼 수정과 똑같이 적용한다 — 전체 값을 보내도 결과가 같다
 */
data class V2TicketItemPatch(
    val payType: TicketPayType? = null,
    val name: String? = null,
    val description: String? = null,
    val price: Long? = null,
    val supplyCount: Long? = null,
    val account: AccountInfoVo? = null,
    val approvalRequired: Boolean? = null,
    val isQuantityPublic: Boolean? = null,
    val purchaseLimit: Long? = null,
    val saleStartAt: LocalDateTime? = null,
    val saleEndAt: LocalDateTime? = null,
    val clear: Set<V2TicketClearableField> = emptySet(),
)

/** T-3 에서 '값 없음'으로 비울 수 있는 필드 */
enum class V2TicketClearableField {
    /** 판매 수량 무제한 */
    SUPPLY_COUNT,

    /** 1인 구매 매수 제한 없음 */
    PURCHASE_LIMIT,

    /** 판매 시작 = 등록 즉시 */
    SALE_START_AT,

    /** 판매 종료 = 공연 시작까지 */
    SALE_END_AT,

    /** 설명 없음 */
    DESCRIPTION,
}

/** 관리 화면 티켓 상태 (01-호스팅센터 6-3) */
enum class V2TicketSaleState {
    /** 재고 감소 없음: 모든 필드 수정·삭제 가능 */
    BEFORE_SALE,

    /** 재고 감소 발생 (v1 isSold): 일부 필드만 수정, 삭제 불가 */
    SOLD,

    /** 판매 중단 (isSellable=false). 판매 여부는 [band.gosrock.domain.domains.ticket_item.domain.TicketItem.isSold] 로 따로 본다 */
    SUSPENDED,
}
