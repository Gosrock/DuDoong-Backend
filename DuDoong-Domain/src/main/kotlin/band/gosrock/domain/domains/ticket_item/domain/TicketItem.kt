package band.gosrock.domain.domains.ticket_item.domain

import band.gosrock.common.consts.DuDoongStatic.MINIMUM_PAYMENT_WON
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.exception.DuplicatedItemOptionGroupException
import band.gosrock.domain.domains.ticket_item.exception.EmptyAccountInfoException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenTicketItemDeleteException
import band.gosrock.domain.domains.ticket_item.exception.InvalidPartnerException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketPriceException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketTypeException
import band.gosrock.domain.domains.ticket_item.exception.NotAppliedItemOptionGroupException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotOnSaleException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLargeException
import band.gosrock.domain.domains.ticket_item.exception.TicketPurchaseLimitException
import java.time.LocalDateTime
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault
import org.springframework.util.StringUtils

@Entity(name = "tbl_ticket_item")
class TicketItem(
    // 티켓 지불 타입
    @Enumerated(EnumType.STRING)
    var payType: TicketPayType? = null,
    // 티켓 이름
    var name: String? = null,
    // 티켓 설명
    var description: String? = null,
    // 티켓 가격
    var price: Money? = null,
    // 티켓 재고
    var quantity: Long? = null,
    // 티켓 공급량
    var supplyCount: Long? = null,
    // 1인당 구매 매수 제한
    var purchaseLimit: Long? = null,
    // 티켓 승인 타입
    @Enumerated(EnumType.STRING)
    var type: TicketType? = null,
    bankName: String? = null,
    accountNumber: String? = null,
    accountHolder: String? = null,
    // 재고 공개 여부
    var isQuantityPublic: Boolean? = null,
    // 판매 가능 여부
    var isSellable: Boolean? = null,
    // 판매 시작 시간
    var saleStartAt: LocalDateTime? = null,
    // 판매 종료 시간
    var saleEndAt: LocalDateTime? = null,
    var eventId: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_item_id")
    var id: Long? = null
        protected set

    @Embedded
    var accountInfo: AccountInfoVo? = if (payType == TicketPayType.DUDOONG_TICKET)
        AccountInfoVo.valueOf(bankName, accountNumber, accountHolder)
    else null
        protected set

    // 상태
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'VALID'")
    var ticketItemStatus: TicketItemStatus = TicketItemStatus.VALID
        protected set

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    val itemOptionGroups: MutableList<ItemOptionGroup> = mutableListOf()

    fun addItemOptionGroup(optionGroup: OptionGroup) {
        // 재고 감소된 티켓상품은 옵션적용 변경 불가
        if (isQuantityReduced()) throw ForbiddenOptionChangeException.EXCEPTION

        // 무료티켓에 유료 옵션 적용 불가
        if (payType == TicketPayType.FREE_TICKET &&
            optionGroup.options.any { it.additionalPrice?.isGreaterThan(Money.ZERO) == true }
        ) {
            throw ForbiddenOptionPriceException.EXCEPTION
        }

        // 중복 체크
        if (hasItemOptionGroup(optionGroup.id!!)) throw DuplicatedItemOptionGroupException.EXCEPTION

        val itemOptionGroup = ItemOptionGroup(item = this, optionGroup = optionGroup)
        this.itemOptionGroups.add(itemOptionGroup)
    }

    fun removeItemOptionGroup(optionGroup: OptionGroup) {
        // 재고 감소된 티켓상품은 옵션적용 변경 불가
        if (isQuantityReduced()) throw ForbiddenOptionChangeException.EXCEPTION
        val itemOptionGroup = findItemOptionGroup(optionGroup)
        this.itemOptionGroups.remove(itemOptionGroup)
    }

    fun findItemOptionGroup(optionGroup: OptionGroup): ItemOptionGroup =
        this.itemOptionGroups.firstOrNull { it.optionGroup == optionGroup }
            ?: throw NotAppliedItemOptionGroupException.EXCEPTION

    fun hasItemOptionGroup(optionGroupId: Long): Boolean =
        this.itemOptionGroups.any { it.optionGroup?.id == optionGroupId }

    fun softDeleteTicketItem() {
        // 재고 감소된 티켓상품은 삭제 불가
        if (isQuantityReduced()) throw ForbiddenTicketItemDeleteException.EXCEPTION
        this.ticketItemStatus = TicketItemStatus.DELETED
    }

    fun validateEventId(eventId: Long) {
        if (this.eventId != eventId) throw InvalidTicketItemException.EXCEPTION
    }

    fun validateTicketPayType(isPartner: Boolean) {
        // 두둥티켓은 무조건 승인 + 계좌정보 필요
        if (payType == TicketPayType.DUDOONG_TICKET) {
            if (type != TicketType.APPROVAL) throw InvalidTicketTypeException.EXCEPTION
            // 두둥 티켓은 유로 티켓 이어야함.
            if (price?.isLessThanOrEqual(Money.ZERO) == true) throw InvalidTicketPriceException.EXCEPTION
            if (!StringUtils.hasText(accountInfo?.bankName) ||
                !StringUtils.hasText(accountInfo?.accountNumber) ||
                !StringUtils.hasText(accountInfo?.accountHolder)
            ) {
                throw EmptyAccountInfoException.EXCEPTION
            }
        }
        // 유료티켓은 무조건 선착순 + 제휴 확인 + 1000원 이상
        else if (payType == TicketPayType.PRICE_TICKET) {
            if (type != TicketType.FIRST_COME_FIRST_SERVED) throw InvalidTicketTypeException.EXCEPTION
            if (!isPartner) throw InvalidPartnerException.EXCEPTION
            if (price?.isLessThan(Money.wons(MINIMUM_PAYMENT_WON)) == true) throw InvalidTicketPriceException.EXCEPTION
        }
        // 무료티켓은 무조건 0원
        else {
            if (price != Money.ZERO) throw InvalidTicketPriceException.EXCEPTION
        }
    }

    /** 선착순 결제인지 확인하는 메서드 */
    fun isFCFS(): Boolean = this.type?.isFCFS() ?: false

    fun hasOption(): Boolean = itemOptionGroups.isNotEmpty()

    fun getOptionGroupIds(): List<Long> =
        itemOptionGroups.mapNotNull { it.optionGroup?.id }.sorted()

    fun isQuantityReduced(): Boolean = quantity != supplyCount

    fun reduceQuantity(quantity: Long?) {
        val qty = quantity ?: 0L
        if (this.quantity!! < 0) throw TicketItemQuantityException.EXCEPTION
        validEnoughQuantity(qty)
        this.quantity = this.quantity!! - qty
    }

    fun validEnoughQuantity(quantity: Long?) {
        val qty = quantity ?: 0L
        if (this.quantity!! < qty) throw TicketItemQuantityLackException.EXCEPTION
    }

    fun validPurchaseLimit(quantity: Long?) {
        if (isPurchaseLimitExceed(quantity ?: 0L)) throw TicketPurchaseLimitException.EXCEPTION
    }

    fun isPurchaseLimitExceed(quantity: Long): Boolean = purchaseLimit!! < quantity

    fun increaseQuantity(quantity: Long) {
        if (this.quantity!! + quantity > supplyCount!!) throw TicketItemQuantityLargeException.EXCEPTION
        this.quantity = this.quantity!! + quantity
    }

    fun isSold(): Boolean = quantity!! < supplyCount!!

    fun isQuantityLeft(): Boolean = quantity!! > 0

    /** 무제한 공급량(v2) 저장값 이상인지. v1 응답·어드민·v2 가 같은 기준([UNLIMITED_SUPPLY_COUNT])을 쓴다 */
    fun isUnlimitedSupply(): Boolean = (supplyCount ?: 0L) >= UNLIMITED_SUPPLY_COUNT

    /** 1인 매수 제한 없음(v2) 저장값 이상인지 ([NO_PURCHASE_LIMIT]) */
    fun hasNoPurchaseLimit(): Boolean = (purchaseLimit ?: 0L) >= NO_PURCHASE_LIMIT

    /**
     * 판매 중인지 (v1/v2 공통 구매 조건): 판매 중단(isSellable=false)이 아니고, 판매 기간이 설정돼 있으면 그 안.
     * v1 로 만든 티켓은 isSellable=true · 판매 기간 null 이라 항상 true. 재고·공연 상태·공연 시작 전 여부는 별도 검증
     */
    fun isOnSale(now: LocalDateTime): Boolean =
        isSellable != false &&
            saleStartAt.let { it == null || !now.isBefore(it) } &&
            saleEndAt.let { it == null || now.isBefore(it) }

    fun validateOnSale(now: LocalDateTime = LocalDateTime.now()) {
        if (!isOnSale(now)) throw TicketItemNotOnSaleException.EXCEPTION
    }

    /** v2 전용 최소 mutator: 계좌 (두둥티켓만, 그 외는 null). 검증은 V2TicketItemDomainService */
    internal fun changeAccountInfo(accountInfo: AccountInfoVo?) {
        this.accountInfo = accountInfo
    }

    /** v2 전용 최소 mutator: 공급량 변경. 판매된 만큼(공급량 - 재고)은 유지하고 재고를 같이 옮긴다 */
    internal fun changeSupplyCount(newSupplyCount: Long) {
        val soldCount = this.supplyCount!! - this.quantity!!
        if (newSupplyCount < soldCount) throw TicketItemQuantityException.EXCEPTION
        this.supplyCount = newSupplyCount
        this.quantity = newSupplyCount - soldCount
    }

    /** 어드민 전용: 재고(quantity)와 공급량(supplyCount)을 동시에 조정 */
    fun adminAdjustStock(delta: Long) {
        // 무제한 티켓은 수량 조정이 의미 없으므로 저장값을 유지한다
        if (isUnlimitedSupply()) return
        val newQuantity = this.quantity!! + delta
        val newSupplyCount = this.supplyCount!! + delta
        if (newQuantity < 0) throw TicketItemQuantityException.EXCEPTION
        if (newSupplyCount < 0) throw TicketItemQuantityException.EXCEPTION
        this.quantity = newQuantity
        this.supplyCount = newSupplyCount
    }

    /** 어드민 전용: 이벤트 상태 체크 없이 티켓 종류 정보 수정 */
    fun adminUpdate(
        name: String?,
        description: String?,
        price: Money?,
        quantity: Long?,
        purchaseLimit: Long?,
    ) {
        if (name != null) this.name = name
        if (description != null) this.description = description
        if (price != null) this.price = price
        if (quantity != null) this.quantity = quantity
        if (purchaseLimit != null) this.purchaseLimit = purchaseLimit
    }

    companion object {
        /**
         * 무제한 공급량 저장값. v1 은 공급량(supplyCount)·재고가 필수라 큰 수로 저장하고, 이 값 이상이면 무제한으로 표시한다.
         * v1 응답(`isUnlimitedSupply`)·어드민·v2 공통 기준 (prod 최대 공급량 1,000 — 2026-10-04 확인)
         */
        const val UNLIMITED_SUPPLY_COUNT = 1_000_000L

        /** 1인 매수 제한 없음 저장값 (v1 은 purchaseLimit 필수). 이 값 이상이면 제한 없음 */
        const val NO_PURCHASE_LIMIT = 1_000_000L
    }
}
