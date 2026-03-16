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
class TicketItem() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_item_id")
    var id: Long? = null
        protected set

    // 티켓 지불 타입
    @Enumerated(EnumType.STRING)
    var payType: TicketPayType? = null
        protected set

    // 티켓 이름
    var name: String? = null
        protected set

    // 티켓 설명
    var description: String? = null
        protected set

    // 티켓 가격
    var price: Money? = null
        protected set

    // 티켓 재고
    var quantity: Long? = null
        protected set

    // 티켓 공급량
    var supplyCount: Long? = null
        protected set

    // 1인당 구매 매수 제한
    var purchaseLimit: Long? = null
        protected set

    // 티켓 승인 타입
    @Enumerated(EnumType.STRING)
    var type: TicketType? = null
        protected set

    @Embedded
    var accountInfo: AccountInfoVo? = null
        protected set

    // 재고 공개 여부
    var isQuantityPublic: Boolean? = null
        protected set

    // 판매 가능 여부
    var isSellable: Boolean? = null
        protected set

    // 판매 시작 시간
    var saleStartAt: LocalDateTime? = null
        protected set

    // 판매 종료 시간
    var saleEndAt: LocalDateTime? = null
        protected set

    // 상태
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'VALID'")
    var ticketItemStatus: TicketItemStatus = TicketItemStatus.VALID
        protected set

    var eventId: Long? = null
        protected set

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    val itemOptionGroups: MutableList<ItemOptionGroup> = mutableListOf()

    constructor(
        payType: TicketPayType?,
        name: String?,
        description: String?,
        price: Money?,
        quantity: Long?,
        supplyCount: Long?,
        purchaseLimit: Long?,
        type: TicketType?,
        bankName: String?,
        accountNumber: String?,
        accountHolder: String?,
        isQuantityPublic: Boolean?,
        isSellable: Boolean?,
        saleStartAt: LocalDateTime?,
        saleEndAt: LocalDateTime?,
        eventId: Long?,
    ) : this() {
        this.payType = payType
        this.name = name
        this.description = description
        this.price = price
        this.quantity = quantity
        this.supplyCount = supplyCount
        this.purchaseLimit = purchaseLimit
        this.type = type
        this.accountInfo = if (payType == TicketPayType.DUDOONG_TICKET)
            AccountInfoVo.valueOf(bankName, accountNumber, accountHolder)
        else null
        this.isQuantityPublic = isQuantityPublic
        this.isSellable = isSellable
        this.saleStartAt = saleStartAt
        this.saleEndAt = saleEndAt
        this.eventId = eventId
    }

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

        val itemOptionGroup = ItemOptionGroup.builder().item(this).optionGroup(optionGroup).build()
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

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var payType: TicketPayType? = null
        private var name: String? = null
        private var description: String? = null
        private var price: Money? = null
        private var quantity: Long? = null
        private var supplyCount: Long? = null
        private var purchaseLimit: Long? = null
        private var type: TicketType? = null
        private var bankName: String? = null
        private var accountNumber: String? = null
        private var accountHolder: String? = null
        private var isQuantityPublic: Boolean? = null
        private var isSellable: Boolean? = null
        private var saleStartAt: LocalDateTime? = null
        private var saleEndAt: LocalDateTime? = null
        private var eventId: Long? = null

        fun payType(payType: TicketPayType?) = apply { this.payType = payType }
        fun name(name: String?) = apply { this.name = name }
        fun description(description: String?) = apply { this.description = description }
        fun price(price: Money?) = apply { this.price = price }
        fun quantity(quantity: Long?) = apply { this.quantity = quantity }
        fun supplyCount(supplyCount: Long?) = apply { this.supplyCount = supplyCount }
        fun purchaseLimit(purchaseLimit: Long?) = apply { this.purchaseLimit = purchaseLimit }
        fun type(type: TicketType?) = apply { this.type = type }
        fun bankName(bankName: String?) = apply { this.bankName = bankName }
        fun accountNumber(accountNumber: String?) = apply { this.accountNumber = accountNumber }
        fun accountHolder(accountHolder: String?) = apply { this.accountHolder = accountHolder }
        fun isQuantityPublic(isQuantityPublic: Boolean?) = apply { this.isQuantityPublic = isQuantityPublic }
        fun isSellable(isSellable: Boolean?) = apply { this.isSellable = isSellable }
        fun saleStartAt(saleStartAt: LocalDateTime?) = apply { this.saleStartAt = saleStartAt }
        fun saleEndAt(saleEndAt: LocalDateTime?) = apply { this.saleEndAt = saleEndAt }
        fun eventId(eventId: Long?) = apply { this.eventId = eventId }
        fun build() = TicketItem(
            payType, name, description, price, quantity, supplyCount, purchaseLimit, type,
            bankName, accountNumber, accountHolder, isQuantityPublic, isSellable,
            saleStartAt, saleEndAt, eventId
        )
    }
}
