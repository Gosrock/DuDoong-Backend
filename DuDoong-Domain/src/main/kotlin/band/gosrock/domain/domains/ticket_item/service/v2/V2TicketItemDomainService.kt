package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItem.Companion.NO_PURCHASE_LIMIT
import band.gosrock.domain.domains.ticket_item.domain.TicketItem.Companion.UNLIMITED_SUPPLY_COUNT
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.exception.EmptyAccountInfoException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenSoldTicketItemChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenTicketItemDeleteException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionGroupException
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemFieldException
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
 * 잠금(DEC-006 / DEC-012 / 옵션 변경): 재고 감소(v1 isSold) 또는 승인 대기(PENDING_APPROVE) 주문이 있는 티켓. [isLocked]
 * 결제 대기(PENDING_PAYMENT)는 만료 기준(배치·시간)이 코드에 없어(DEC-004) 판정에서 제외한다.
 *
 * 티켓을 바꾸는 메서드는 v1 재고 감소(IssuedTicketDomainService)와 같은 락(`티켓관리:{ticketItemId}`)을 잡고 새 트랜잭션에서 처리한다.
 */
@DomainService
@Transactional(readOnly = true)
class V2TicketItemDomainService(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
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

    /**
     * 사용자 앱(P-5)에서 지금 살 수 있는지: v2 주문이 지원하는 결제 방식(DUDOONG / FREE) + [isPurchasable] + 승인 대기를 뺀 잔여([availableQuantity]) > 0.
     * 기존 PG 티켓(PRICE)은 v2 주문 경로가 없어 false (#716 L-5). 관리 화면(T-1)의 isPurchasable 은 결제 방식과 무관하게 [isPurchasable]
     */
    fun isPurchasableInV2App(item: TicketItem, event: Event, now: LocalDateTime, pendingApproveQuantity: Long): Boolean =
        item.payType in V2_ORDER_PAY_TYPES && isPurchasable(item, event, now) && availableQuantity(item, pendingApproveQuantity) > 0

    /**
     * 사용자에게 보이는 잔여 = 재고 - 승인 대기 수량 (0 미만은 0, #726). 승인 전에는 재고가 줄지 않으므로(DEC-020 #1) 빼서 보여 준다.
     * 승인형 주문 생성 재고 검사(v1 `OrderValidator.validApproveOrderCreateTotalStock`, #723: 같은 티켓 승인 대기 + 이번 수량 <= 재고)와 같은 기준이라,
     * 잔여 n 이면 n 장까지 주문이 들어간다
     */
    fun availableQuantity(item: TicketItem, pendingApproveQuantity: Long): Long = maxOf(0L, item.quantity!! - pendingApproveQuantity)

    /** 티켓별 승인 대기 수량 (한 번의 그룹 쿼리). 승인 대기가 없는 티켓은 맵에 없다(= 0) */
    fun pendingApproveQuantities(itemIds: Collection<Long>): Map<Long, Long> = orderAdaptor.sumPendingApproveQuantities(itemIds)

    /** 잠김: 재고 감소(판매됨) 또는 승인 대기 주문 있음. 잠기면 금액·결제 조건 필드와 옵션을 바꿀 수 없다 */
    fun isLocked(item: TicketItem, hasPendingOrders: Boolean): Boolean = item.isSold() || hasPendingOrders

    /** 승인 대기 주문이 있는 티켓 id (한 번의 쿼리) */
    fun pendingOrderItemIds(itemIds: Collection<Long>): Set<Long> = orderAdaptor.findItemIdsHavingPendingApproveOrder(itemIds)

    fun hasPendingOrders(ticketItemId: Long): Boolean = pendingOrderItemIds(listOf(ticketItemId)).isNotEmpty()

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
        validateForm(event, normalized, current = null, now = now)
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
            isQuantityPublic = normalized.isQuantityPublic,
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
     * 폼 전체 수정. 판매 전이면 모든 필드, 잠김([isLocked])이면 설명·판매기간·재고공개·매수제한·수량 증가만 (DEC-006).
     * 잠긴 필드는 저장값과 요청값을 양쪽 trim 해 비교하고 다를 때만 400 이라, 수정 화면이 전체 값을 그대로 보내도 된다.
     * 바뀌지 않은 이름·설명은 길이 검증 없이 저장값 그대로 둔다 (v1 에서 만든 긴 이름 호환)
     */
    fun applyUpdate(item: TicketItem, event: Event, form: V2TicketItemForm, hasPendingOrders: Boolean, now: LocalDateTime) {
        v2EventDomainService.validateEditable(event)
        if (item.payType == TicketPayType.PRICE_TICKET) throw UnsupportedV2TicketPayTypeException.EXCEPTION
        val normalized = normalize(form)
        validateForm(event, normalized, current = item, now = now)
        // 현재 무제한이고 폼도 무제한이면 저장값 유지 (어드민 조정 등으로 저장값보다 클 수 있음)
        val newSupplyCount = normalized.supplyCount ?: if (item.isUnlimitedSupply()) item.supplyCount!! else UNLIMITED_SUPPLY_COUNT
        val newPrice = Money.wons(normalized.price)
        val newType = ticketTypeOf(normalized)
        val nameChanged = item.name?.trim() != normalized.name
        val descriptionChanged = item.description?.trim()?.ifEmpty { null } != normalized.description
        val accountChanged = !sameAccount(item.accountInfo, normalized.account)
        if (isLocked(item, hasPendingOrders)) {
            val lockedChanged = item.payType != normalized.payType || nameChanged || item.price != newPrice || item.type != newType || accountChanged
            if (lockedChanged || newSupplyCount < item.supplyCount!!) throw ForbiddenSoldTicketItemChangeException.EXCEPTION
        }
        // 무료티켓에 유료 옵션 불가 (v1 옵션 적용 규칙과 같음)
        if (normalized.payType == TicketPayType.FREE_TICKET && hasPaidOption(item)) throw ForbiddenOptionPriceException.EXCEPTION

        item.payType = normalized.payType
        if (nameChanged) item.name = normalized.name
        if (descriptionChanged) item.description = normalized.description
        item.price = newPrice
        item.type = newType
        if (accountChanged || normalized.account == null) item.changeAccountInfo(normalized.account)
        item.isQuantityPublic = normalized.isQuantityPublic
        item.purchaseLimit = normalized.purchaseLimit ?: if (item.hasNoPurchaseLimit()) item.purchaseLimit else NO_PURCHASE_LIMIT
        item.saleStartAt = normalized.saleStartAt
        item.saleEndAt = normalized.saleEndAt
        if (newSupplyCount != item.supplyCount) item.changeSupplyCount(newSupplyCount)
        // 저장 직전 v1 결제 방식 규칙(두둥=승인+계좌+유료, 무료=0원)도 다시 확인
        item.validateTicketPayType(false)
    }

    @RedissonLock(LockName = TICKET_LOCK, identifier = "ticketItemId")
    fun updateTicketItem(eventId: Long, ticketItemId: Long, form: V2TicketItemForm, now: LocalDateTime) {
        val item = queryTicketItem(eventId, ticketItemId)
        applyUpdate(item, eventAdaptor.findById(eventId), form, hasPendingOrders(ticketItemId), now)
        ticketItemAdaptor.save(item)
    }

    /**
     * 삭제: 잠기지 않은 티켓만 (재고 감소 OR 승인 대기 주문이면 Ticket_Item_400_7, v1 '삭제 불가' 코드 재사용).
     * 재고 감소 검사는 v1 과 같은 [TicketItem.softDeleteTicketItem]. 승인 대기 검사는 v2 에만 있다 (v1 삭제 규칙 불변).
     * 승인 처리(재고 감소)와 같은 락 안에서 판정·삭제한다
     */
    @RedissonLock(LockName = TICKET_LOCK, identifier = "ticketItemId")
    fun deleteTicketItem(eventId: Long, ticketItemId: Long) {
        v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
        val item = queryTicketItem(eventId, ticketItemId)
        if (hasPendingOrders(ticketItemId)) throw ForbiddenTicketItemDeleteException.EXCEPTION
        item.softDeleteTicketItem()
        ticketItemAdaptor.save(item)
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
     * 티켓에 붙은 옵션 전체 지정. 중복 id 는 하나로. 잠긴 티켓(재고 감소·승인 대기)은 같은 목록만 허용 (Item_Option_Group_400_2).
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
        if (isLocked(item, hasPendingOrders(ticketItemId))) throw ForbiddenOptionChangeException.EXCEPTION

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

    /** @param current 수정이면 현재 티켓. 이름·설명 길이는 새로 만들거나 값이 바뀔 때만 검증한다 */
    private fun validateForm(event: Event, form: V2TicketItemForm, current: TicketItem?, now: LocalDateTime) {
        when (form.payType) {
            TicketPayType.DUDOONG_TICKET -> {
                if (form.price <= 0 || form.price > MAX_PRICE) throw InvalidTicketPriceException.EXCEPTION
                val account = form.account
                if (account == null || account.bankName.isNullOrBlank() || account.accountNumber.isNullOrBlank() || account.accountHolder.isNullOrBlank()) {
                    throw EmptyAccountInfoException.EXCEPTION
                }
            }
            TicketPayType.FREE_TICKET -> if (form.price != 0L) throw InvalidTicketPriceException.EXCEPTION
            TicketPayType.PRICE_TICKET -> throw UnsupportedV2TicketPayTypeException.EXCEPTION
        }
        if (form.name.isEmpty()) throw InvalidTicketItemFieldException.EXCEPTION
        if ((current == null || current.name?.trim() != form.name) && form.name.length > NAME_MAX_LENGTH) throw InvalidTicketItemFieldException.EXCEPTION
        val description = form.description
        if (description != null && (current == null || current.description?.trim() != description) && description.length > DESCRIPTION_MAX_LENGTH) {
            throw InvalidTicketItemFieldException.EXCEPTION
        }
        form.supplyCount?.let { if (it < 1 || it > MAX_SUPPLY_COUNT) throw TicketItemQuantityException.EXCEPTION }
        form.purchaseLimit?.let { if (it < 1 || it > MAX_SUPPLY_COUNT) throw TicketItemQuantityException.EXCEPTION }
        // 무제한 티켓은 잔여 매수가 의미 없다 (v1 공개 목록에 저장값이 보이지 않도록)
        if (form.supplyCount == null && form.isQuantityPublic) throw InvalidTicketItemFieldException.EXCEPTION
        validateSalePeriod(event, form.saleStartAt, form.saleEndAt, current?.saleEndAt, now)
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

    private fun sameAccount(current: AccountInfoVo?, new: AccountInfoVo?): Boolean =
        current?.bankName.orEmpty().trim() == new?.bankName.orEmpty().trim() &&
            current?.accountNumber.orEmpty().trim() == new?.accountNumber.orEmpty().trim() &&
            current?.accountHolder.orEmpty().trim() == new?.accountHolder.orEmpty().trim()

    private fun hasPaidOption(item: TicketItem): Boolean =
        item.itemOptionGroups.mapNotNull { it.optionGroup }
            .flatMap { it.options }
            .any { it.additionalPrice?.isGreaterThan(Money.ZERO) == true }

    companion object {
        /** v2 주문이 지원하는 결제 방식 (계좌송금 두둥티켓 / 무료) */
        val V2_ORDER_PAY_TYPES: Set<TicketPayType> = setOf(TicketPayType.DUDOONG_TICKET, TicketPayType.FREE_TICKET)

        private const val TICKET_LOCK = "티켓관리"

        /** 수량 지정·매수 제한 최대값 (무제한 저장값 [TicketItem.UNLIMITED_SUPPLY_COUNT] 보다 작아야 한다) */
        const val MAX_SUPPLY_COUNT = 999_999L

        /** 티켓 가격·옵션 추가금 상한(원) */
        const val MAX_PRICE = 10_000_000L

        const val NAME_MAX_LENGTH = 12
        const val DESCRIPTION_MAX_LENGTH = 30
        const val MAX_OPTION_COUNT = 10
    }
}
