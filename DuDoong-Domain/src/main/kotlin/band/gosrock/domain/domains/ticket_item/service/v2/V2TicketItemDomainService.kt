package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.exception.EmptyAccountInfoException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenSoldTicketItemChangeException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionGroupException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketPriceException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketSalePeriodException
import band.gosrock.domain.domains.ticket_item.exception.TicketDisabledEventException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotFoundException
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityException
import band.gosrock.domain.domains.ticket_item.exception.UnsupportedV2TicketPayTypeException
import band.gosrock.domain.domains.ticket_item.service.TicketItemService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.springframework.transaction.annotation.Transactional

/**
 * v2 전용 티켓 규칙 (DEC-018). v1 코드는 이 서비스를 호출하지 않는다.
 * v1/v2 공통 불변식(재고 감소 = 판매됨, 판매된 티켓 옵션 변경·삭제 불가, 판매 중·기간 검사)은 [TicketItem] 에 있다.
 *
 * 티켓을 바꾸는 메서드는 v1 재고 감소(IssuedTicketDomainService)와 같은 락(`티켓관리:{ticketItemId}`)을 잡고 새 트랜잭션에서 처리한다.
 */
@DomainService
@Transactional(readOnly = true)
class V2TicketItemDomainService(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val ticketItemService: TicketItemService,
    private val v2EventDomainService: V2EventDomainService,
) {

    /** 관리 화면 상태. 판매 중단이 먼저, 그다음 재고 감소 여부 */
    fun saleState(item: TicketItem): V2TicketSaleState = when {
        item.isSellable == false -> V2TicketSaleState.SUSPENDED
        item.isSold() -> V2TicketSaleState.SOLD
        else -> V2TicketSaleState.BEFORE_SALE
    }

    /** 지금 사용자가 살 수 있는지: 판매 중(중단 아님 + 판매 기간) + 공연 등록(OPEN) + 공연 시작 전 + 재고 > 0 */
    fun isPurchasable(item: TicketItem, event: Event, now: LocalDateTime): Boolean {
        val startAt = event.getStartAt() ?: return false
        return item.isOnSale(now) && event.status == EventStatus.OPEN && now.isBefore(startAt) && item.isQuantityLeft()
    }

    fun isUnlimitedSupply(item: TicketItem): Boolean = (item.supplyCount ?: 0L) >= UNLIMITED_SUPPLY_COUNT

    fun hasNoPurchaseLimit(item: TicketItem): Boolean = (item.purchaseLimit ?: 0L) >= NO_PURCHASE_LIMIT

    /** 이 공연의 유효 티켓 조회. 다른 공연 티켓 id 는 존재를 드러내지 않도록 404 */
    fun queryTicketItem(eventId: Long, ticketItemId: Long): TicketItem {
        val item = ticketItemAdaptor.queryTicketItem(ticketItemId)
        if (item.eventId != eventId) throw TicketItemNotFoundException.EXCEPTION
        return item
    }

    /** 생성 (저장 전). 티켓 없음 공연 불가, 정산중·지난 공연 불가 */
    fun newTicketItem(event: Event, form: V2TicketItemForm, now: LocalDateTime): TicketItem {
        v2EventDomainService.validateEditable(event)
        if (!event.hasTicket) throw TicketDisabledEventException.EXCEPTION
        val normalized = normalize(form)
        validateForm(event, normalized, currentSaleEndAt = null, now = now)
        val supplyCount = normalized.supplyCount ?: UNLIMITED_SUPPLY_COUNT
        return TicketItem(
            payType = normalized.payType,
            name = normalized.name,
            description = normalized.description,
            price = Money.wons(normalized.price),
            quantity = supplyCount,
            supplyCount = supplyCount,
            purchaseLimit = normalized.purchaseLimit ?: NO_PURCHASE_LIMIT,
            type = ticketTypeOf(normalized),
            bankName = normalized.account?.bankName,
            accountNumber = normalized.account?.accountNumber,
            accountHolder = normalized.account?.accountHolder,
            isQuantityPublic = quantityPublicOf(normalized),
            isSellable = true,
            saleStartAt = normalized.saleStartAt,
            saleEndAt = normalized.saleEndAt,
            eventId = event.id,
        )
    }

    /** 생성 + 저장. 저장 직전 v1 결제 방식 검증([TicketItem.validateTicketPayType])도 그대로 거친다 */
    @Transactional
    fun createTicketItem(event: Event, form: V2TicketItemForm, now: LocalDateTime): TicketItem =
        ticketItemService.createTicketItem(newTicketItem(event, form, now), false)

    /**
     * 폼 전체 수정. 판매 전이면 모든 필드, 판매됨(재고 감소)이면 설명·판매기간·재고공개·매수제한·수량 증가만 (DEC-006).
     * 잠긴 필드는 현재 값과 다를 때만 400 이라 수정 화면이 전체 값을 그대로 보내도 된다.
     */
    fun applyUpdate(item: TicketItem, event: Event, form: V2TicketItemForm, now: LocalDateTime) {
        v2EventDomainService.validateEditable(event)
        if (item.payType == TicketPayType.PRICE_TICKET) throw UnsupportedV2TicketPayTypeException.EXCEPTION
        val normalized = normalize(form)
        validateForm(event, normalized, currentSaleEndAt = item.saleEndAt, now = now)
        val newSupplyCount = normalized.supplyCount ?: UNLIMITED_SUPPLY_COUNT
        val newPrice = Money.wons(normalized.price)
        val newType = ticketTypeOf(normalized)
        val newAccount = normalized.account
        if (item.isSold()) {
            val lockedChanged = item.payType != normalized.payType ||
                item.name != normalized.name ||
                item.price != newPrice ||
                item.type != newType ||
                !sameAccount(item.accountInfo, newAccount)
            if (lockedChanged || newSupplyCount < item.supplyCount!!) throw ForbiddenSoldTicketItemChangeException.EXCEPTION
        }
        // 무료티켓에 유료 옵션 불가 (v1 옵션 적용 규칙과 같음)
        if (normalized.payType == TicketPayType.FREE_TICKET && hasPaidOption(item)) throw ForbiddenOptionPriceException.EXCEPTION

        item.payType = normalized.payType
        item.name = normalized.name
        item.description = normalized.description
        item.price = newPrice
        item.type = newType
        item.changeAccountInfo(newAccount)
        item.isQuantityPublic = quantityPublicOf(normalized)
        item.purchaseLimit = normalized.purchaseLimit ?: NO_PURCHASE_LIMIT
        item.saleStartAt = normalized.saleStartAt
        item.saleEndAt = normalized.saleEndAt
        if (newSupplyCount != item.supplyCount) item.changeSupplyCount(newSupplyCount)
    }

    @RedissonLock(LockName = TICKET_LOCK, identifier = "ticketItemId")
    fun updateTicketItem(eventId: Long, ticketItemId: Long, form: V2TicketItemForm, now: LocalDateTime) {
        val item = queryTicketItem(eventId, ticketItemId)
        applyUpdate(item, eventAdaptor.findById(eventId), form, now)
        ticketItemAdaptor.save(item)
    }

    /** 삭제: 판매 전만 (v1 [TicketItemService.softDeleteTicketItem] 규칙·락 그대로) */
    fun deleteTicketItem(eventId: Long, ticketItemId: Long) {
        v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
        queryTicketItem(eventId, ticketItemId)
        ticketItemService.softDeleteTicketItem(eventId, ticketItemId)
    }

    /** 판매 중단 / 재개. 이미 그 상태면 아무것도 하지 않는다 (멱등) */
    @RedissonLock(LockName = TICKET_LOCK, identifier = "ticketItemId")
    fun changeSellable(eventId: Long, ticketItemId: Long, sellable: Boolean) {
        v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
        val item = queryTicketItem(eventId, ticketItemId)
        if (item.isSellable == sellable) return
        item.isSellable = sellable
        ticketItemAdaptor.save(item)
    }

    /**
     * 티켓에 붙은 옵션 전체 지정. 중복 id 는 하나로. 판매된 티켓은 같은 목록만 허용 (Item_Option_Group_400_2).
     * 다른 공연 옵션은 400 (Option_Group_400_1), 없는·삭제된 옵션은 404
     */
    @RedissonLock(LockName = TICKET_LOCK, identifier = "ticketItemId")
    fun replaceOptions(eventId: Long, ticketItemId: Long, optionGroupIds: List<Long>) {
        v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
        val item = queryTicketItem(eventId, ticketItemId)
        val distinctIds = optionGroupIds.distinct()
        val optionGroups = distinctIds.map { optionGroupAdaptor.queryOptionGroup(it) }
        optionGroups.forEach { if (it.eventId != eventId) throw InvalidOptionGroupException.EXCEPTION }
        if (distinctIds.toSet() == item.getOptionGroupIds().toSet()) return
        if (item.isSold()) throw ForbiddenOptionChangeException.EXCEPTION

        item.itemOptionGroups.mapNotNull { it.optionGroup }
            .filter { it.id !in distinctIds }
            .forEach { item.removeItemOptionGroup(it) }
        optionGroups.filter { !item.hasItemOptionGroup(it.id!!) }
            .forEach { item.addItemOptionGroup(it) }
        ticketItemAdaptor.save(item)
    }

    private fun normalize(form: V2TicketItemForm): V2TicketItemForm =
        form.copy(
            name = form.name.trim(),
            description = form.description?.trim()?.ifEmpty { null },
            // 두둥티켓은 계좌 필수 + 항상 승인, 그 외는 계좌를 저장하지 않는다
            account = if (form.payType == TicketPayType.DUDOONG_TICKET) form.account?.let {
                AccountInfoVo(bankName = it.bankName?.trim(), accountNumber = it.accountNumber?.trim(), accountHolder = it.accountHolder?.trim())
            } else null,
            approvalRequired = form.payType == TicketPayType.DUDOONG_TICKET || form.approvalRequired,
            saleStartAt = form.saleStartAt?.truncatedTo(ChronoUnit.MINUTES),
            saleEndAt = form.saleEndAt?.truncatedTo(ChronoUnit.MINUTES),
        )

    private fun validateForm(event: Event, form: V2TicketItemForm, currentSaleEndAt: LocalDateTime?, now: LocalDateTime) {
        when (form.payType) {
            TicketPayType.DUDOONG_TICKET -> {
                if (form.price <= 0) throw InvalidTicketPriceException.EXCEPTION
                val account = form.account
                if (account == null || account.bankName.isNullOrBlank() || account.accountNumber.isNullOrBlank() || account.accountHolder.isNullOrBlank()) {
                    throw EmptyAccountInfoException.EXCEPTION
                }
            }
            TicketPayType.FREE_TICKET -> if (form.price != 0L) throw InvalidTicketPriceException.EXCEPTION
            TicketPayType.PRICE_TICKET -> throw UnsupportedV2TicketPayTypeException.EXCEPTION
        }
        form.supplyCount?.let { if (it < 1 || it > MAX_SUPPLY_COUNT) throw TicketItemQuantityException.EXCEPTION }
        form.purchaseLimit?.let { if (it < 1 || it > MAX_SUPPLY_COUNT) throw TicketItemQuantityException.EXCEPTION }
        validateSalePeriod(event, form.saleStartAt, form.saleEndAt, currentSaleEndAt, now)
    }

    /**
     * 판매 기간: 시작 < 종료, 둘 다 공연 시작 이전(종료는 공연 시작과 같아도 됨).
     * 새로 정한 종료는 현재 이후여야 한다 (기존 값 그대로면 지나도 허용 — 다른 필드만 고치는 경우)
     */
    fun validateSalePeriod(
        event: Event,
        saleStartAt: LocalDateTime?,
        saleEndAt: LocalDateTime?,
        currentSaleEndAt: LocalDateTime?,
        now: LocalDateTime,
    ) {
        val eventStartAt = event.getStartAt() ?: throw InvalidTicketSalePeriodException.EXCEPTION
        if (saleStartAt != null && saleEndAt != null && !saleStartAt.isBefore(saleEndAt)) throw InvalidTicketSalePeriodException.EXCEPTION
        if (saleStartAt != null && !saleStartAt.isBefore(eventStartAt)) throw InvalidTicketSalePeriodException.EXCEPTION
        if (saleEndAt != null && saleEndAt.isAfter(eventStartAt)) throw InvalidTicketSalePeriodException.EXCEPTION
        if (saleEndAt != null && saleEndAt != currentSaleEndAt && !saleEndAt.isAfter(now)) throw InvalidTicketSalePeriodException.EXCEPTION
    }

    private fun ticketTypeOf(form: V2TicketItemForm): TicketType =
        if (form.approvalRequired) TicketType.APPROVAL else TicketType.FIRST_COME_FIRST_SERVED

    /** 무제한 티켓은 잔여 매수가 의미 없으므로(v1 공개 목록에 큰 수가 보이지 않도록) 재고 공개를 끈다 */
    private fun quantityPublicOf(form: V2TicketItemForm): Boolean = form.isQuantityPublic && form.supplyCount != null

    private fun sameAccount(current: AccountInfoVo?, new: AccountInfoVo?): Boolean =
        current?.bankName.orEmpty() == new?.bankName.orEmpty() &&
            current?.accountNumber.orEmpty() == new?.accountNumber.orEmpty() &&
            current?.accountHolder.orEmpty() == new?.accountHolder.orEmpty()

    private fun hasPaidOption(item: TicketItem): Boolean =
        item.itemOptionGroups.mapNotNull { it.optionGroup }
            .flatMap { it.options }
            .any { it.additionalPrice?.isGreaterThan(Money.ZERO) == true }

    companion object {
        private const val TICKET_LOCK = "티켓관리"

        /** 무제한 공급량 저장값. v1 은 공급량이 필수라 큰 수로 저장하고, 이 값 이상이면 무제한으로 표시한다 */
        const val UNLIMITED_SUPPLY_COUNT = 1_000_000L

        /** 1인 매수 제한 없음 저장값 (v1 은 필수 값) */
        const val NO_PURCHASE_LIMIT = 1_000_000L

        /** 수량 지정·매수 제한 최대값 (무제한 저장값보다 작아야 한다) */
        const val MAX_SUPPLY_COUNT = 999_999L

        const val NAME_MAX_LENGTH = 12
        const val DESCRIPTION_MAX_LENGTH = 30
        const val MAX_OPTION_COUNT = 10
    }
}
