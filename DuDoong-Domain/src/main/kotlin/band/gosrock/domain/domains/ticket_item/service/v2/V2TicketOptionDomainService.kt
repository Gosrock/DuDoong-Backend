package band.gosrock.domain.domains.ticket_item.service.v2

import band.gosrock.domain.common.aop.redissonLock.LockNames
import band.gosrock.common.annotation.DomainService
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.common.exception.NotAvailableRedissonLockException
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenLockedOptionChangeException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionGroupDeleteException
import band.gosrock.domain.domains.ticket_item.exception.ForbiddenOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionDescriptionException
import band.gosrock.domain.domains.ticket_item.exception.InvalidOptionPriceException
import band.gosrock.domain.domains.ticket_item.exception.OptionGroupNotFoundException
import band.gosrock.domain.domains.ticket_item.exception.UnsupportedV2OptionTypeException
import java.util.concurrent.TimeUnit
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * v2 전용 옵션 규칙 (DEC-018, DEC-012). 옵션 = v1 OptionGroup (YES_NO = TRUE_FALSE, 행은 v1 과 같이 '예'/'아니오' 2개, 주관식 1개).
 * 옵션 행(tbl_option)은 주문 응답이 id 로 참조하므로 지우거나 바꿔 끼우지 않는다. 그래서 응답 형식은 만든 뒤 바꿀 수 없다.
 * 잠금 판정은 [V2TicketItemDomainService.isLocked] 와 같다 (재고 감소 또는 승인 대기 주문).
 * 수정·삭제는 옵션이 붙은 티켓들의 `티켓관리:{ticketItemId}` 락을 id 순으로 모두 잡은 뒤 새 트랜잭션에서 판정·변경한다 (재고 감소와 직렬화, 데드락 방지).
 */
@DomainService
@Transactional(readOnly = true)
class V2TicketOptionDomainService(
    private val optionGroupAdaptor: OptionGroupAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val v2EventDomainService: V2EventDomainService,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val redissonClient: RedissonClient,
    transactionManager: PlatformTransactionManager,
) {
    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        timeout = TRANSACTION_TIMEOUT_SECONDS
    }

    /** 이 공연의 유효 옵션 조회. 다른 공연 옵션 id 는 404 */
    fun queryOptionGroup(eventId: Long, optionGroupId: Long): OptionGroup {
        val optionGroup = optionGroupAdaptor.queryOptionGroup(optionGroupId)
        if (optionGroup.eventId != eventId) throw OptionGroupNotFoundException.EXCEPTION
        return optionGroup
    }

    /** 이 옵션이 붙은 유효 티켓 */
    fun appliedTicketItems(optionGroup: OptionGroup, ticketItems: List<TicketItem>): List<TicketItem> =
        ticketItems.filter { it.hasItemOptionGroup(optionGroup.id!!) }

    /** 잠긴 티켓(재고 감소 또는 승인 대기 주문)에 붙어 있으면 잠김: 이름·설명만 수정, 삭제 불가 */
    fun isLocked(optionGroup: OptionGroup, ticketItems: List<TicketItem>, pendingOrderItemIds: Set<Long>): Boolean =
        appliedTicketItems(optionGroup, ticketItems).any { v2TicketItemDomainService.isLocked(it, it.id in pendingOrderItemIds) }

    /** '예' 선택 시 추가 금액. 네/아니오가 아니면 null */
    fun yesAdditionalPrice(optionGroup: OptionGroup): Long? =
        if (optionGroup.type == OptionGroupType.TRUE_FALSE) {
            (optionGroup.options.firstOrNull { it.answer == KR_YES }?.additionalPrice ?: Money.ZERO).longValue()
        } else null

    /** 생성 (저장 전). 주관식은 추가 금액 없음, 네/아니오는 0 이상 (없으면 0) */
    fun newOptionGroup(event: Event, name: String, description: String, type: OptionGroupType, yesAdditionalPrice: Long?): OptionGroup {
        v2EventDomainService.validateEditable(event)
        if (description.trim().length > DESCRIPTION_MAX_LENGTH) throw InvalidOptionDescriptionException.EXCEPTION
        val price = validatePrice(type, yesAdditionalPrice)
        return OptionGroup(
            eventId = event.id,
            type = type,
            name = name.trim(),
            description = description.trim(),
            isEssential = true,
        ).createTicketOption(Money.wons(price))
    }

    @Transactional
    fun createOptionGroup(event: Event, name: String, description: String, type: OptionGroupType, yesAdditionalPrice: Long?): OptionGroup =
        optionGroupAdaptor.save(newOptionGroup(event, name, description, type, yesAdditionalPrice))

    /**
     * 부분 수정 (null 은 변경 안 함). 응답 형식은 바꿀 수 없다.
     * 잠긴 옵션(판매된 티켓에 붙음)은 이름·설명만, 추가 금액을 다른 값으로 바꾸면 400 (DEC-012).
     * 무료티켓에 붙은 옵션은 추가 금액을 0 보다 크게 할 수 없다 (v1 옵션 적용 규칙과 같음).
     * 설명 길이([DESCRIPTION_MAX_LENGTH])는 값이 바뀔 때만 검증한다 — v1 이 길이 제한 없이 만든 설명을 그대로 다시 보내도 통과 (#752, 티켓 이름·설명과 같은 방식)
     */
    fun applyUpdate(
        optionGroup: OptionGroup,
        ticketItems: List<TicketItem>,
        pendingOrderItemIds: Set<Long>,
        name: String?,
        description: String?,
        yesAdditionalPrice: Long?,
    ) {
        if (yesAdditionalPrice != null) {
            val price = validatePrice(optionGroup.type!!, yesAdditionalPrice)
            // 주관식은 0 만 허용(위에서 검증)되고 바꿀 금액이 없다
            if (optionGroup.type == OptionGroupType.TRUE_FALSE && price != yesAdditionalPrice(optionGroup)) {
                val applied = appliedTicketItems(optionGroup, ticketItems)
                if (isLocked(optionGroup, applied, pendingOrderItemIds)) throw ForbiddenLockedOptionChangeException.EXCEPTION
                if (price > 0 && applied.any { it.payType == TicketPayType.FREE_TICKET }) throw ForbiddenOptionPriceException.EXCEPTION
                optionGroup.options.first { it.answer == KR_YES }.additionalPrice = Money.wons(price)
            }
        }
        if (name != null) optionGroup.name = name.trim()
        if (description != null) {
            val trimmed = description.trim()
            if (trimmed != optionGroup.description?.trim() && trimmed.length > DESCRIPTION_MAX_LENGTH) throw InvalidOptionDescriptionException.EXCEPTION
            optionGroup.description = trimmed
        }
    }

    /** 락 대기 동안 바깥 트랜잭션을 잡지 않는다 (판정·변경은 락 안의 새 트랜잭션) */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun updateOptionGroup(eventId: Long, optionGroupId: Long, name: String?, description: String?, yesAdditionalPrice: Long?) {
        withAppliedTicketLocks(eventId, optionGroupId) { optionGroup, ticketItems ->
            val pending = v2TicketItemDomainService.pendingOrderItemIds(ticketItems.mapNotNull { it.id })
            applyUpdate(optionGroup, ticketItems, pending, name, description, yesAdditionalPrice)
            optionGroupAdaptor.save(optionGroup)
        }
    }

    /** 삭제: 잠긴 티켓에 붙어 있으면 400. 판매 전 티켓에 붙어 있으면 떼어 내고 삭제한다 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun deleteOptionGroup(eventId: Long, optionGroupId: Long) {
        withAppliedTicketLocks(eventId, optionGroupId) { optionGroup, ticketItems ->
            val applied = appliedTicketItems(optionGroup, ticketItems)
            val pending = v2TicketItemDomainService.pendingOrderItemIds(applied.mapNotNull { it.id })
            if (isLocked(optionGroup, applied, pending)) throw ForbiddenOptionGroupDeleteException.EXCEPTION
            applied.forEach {
                it.removeItemOptionGroup(optionGroup)
                ticketItemAdaptor.save(it)
            }
            optionGroup.softDeleteOptionGroup(ticketItems)
            optionGroupAdaptor.save(optionGroup)
        }
    }

    /**
     * 옵션이 붙은 티켓들의 락을 id 오름차순으로 잡고, 새 트랜잭션에서 [block] 을 실행한다.
     * 락을 잡는 사이 옵션이 다른 티켓에 새로 붙었으면(락 밖) 락을 풀고 다시 시도한다.
     */
    private fun withAppliedTicketLocks(eventId: Long, optionGroupId: Long, block: (OptionGroup, List<TicketItem>) -> Unit) {
        repeat(MAX_LOCK_ATTEMPTS) {
            val lockIds = newTransaction.execute { appliedTicketItemIds(eventId, optionGroupId) }!!
            val acquired = mutableListOf<RLock>()
            try {
                lockIds.sorted().forEach { id ->
                    val lock = redissonClient.getLock("$TICKET_LOCK:$id")
                    if (!lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) throw NotAvailableRedissonLockException.EXCEPTION
                    acquired.add(lock)
                }
                val done = newTransaction.execute {
                    v2EventDomainService.validateEditable(eventAdaptor.findById(eventId))
                    val optionGroup = queryOptionGroup(eventId, optionGroupId)
                    val ticketItems = ticketItemAdaptor.findAllByEventId(eventId)
                    if (!lockIds.containsAll(appliedTicketItems(optionGroup, ticketItems).mapNotNull { it.id })) {
                        false
                    } else {
                        block(optionGroup, ticketItems)
                        true
                    }
                }!!
                if (done) return
            } finally {
                acquired.asReversed().forEach { if (it.isHeldByCurrentThread) it.unlock() }
            }
        }
        throw NotAvailableRedissonLockException.EXCEPTION
    }

    private fun appliedTicketItemIds(eventId: Long, optionGroupId: Long): Set<Long> {
        val optionGroup = queryOptionGroup(eventId, optionGroupId)
        return appliedTicketItems(optionGroup, ticketItemAdaptor.findAllByEventId(eventId)).mapNotNull { it.id }.toSet()
    }

    private fun validatePrice(type: OptionGroupType, yesAdditionalPrice: Long?): Long = when (type) {
        OptionGroupType.TRUE_FALSE -> {
            val price = yesAdditionalPrice ?: 0L
            if (price < 0 || price > V2TicketItemDomainService.MAX_PRICE) throw InvalidOptionPriceException.EXCEPTION
            price
        }
        OptionGroupType.SUBJECTIVE -> {
            if (yesAdditionalPrice != null && yesAdditionalPrice != 0L) throw InvalidOptionPriceException.EXCEPTION
            0L
        }
        OptionGroupType.MULTIPLE_CHOICE -> throw UnsupportedV2OptionTypeException.EXCEPTION
    }

    companion object {
        /** v1 재고 감소(IssuedTicketDomainService)·v2 티켓 변경과 같은 락 이름 */
        private const val TICKET_LOCK = LockNames.TICKET
        private const val LOCK_WAIT_SECONDS = 10L
        private const val LOCK_LEASE_SECONDS = 10L

        /** 락 lease 보다 짧게 (RedissonCallNewTransaction 과 같은 기준) */
        private const val TRANSACTION_TIMEOUT_SECONDS = 9
        private const val MAX_LOCK_ATTEMPTS = 3

        const val NAME_MAX_LENGTH = 20
        /** 옵션 설명 (Figma 50자, 사용자 결정 2026-10-09 #752). prod 기존 설명은 모두 50자 이하 (2026-10-09 읽기 전용 확인) */
        const val DESCRIPTION_MAX_LENGTH = 50

        /** 저장 컬럼 길이. [DESCRIPTION_MAX_LENGTH] 는 도메인이 trim 후 검사하므로(생성 O-2, 수정 O-3 은 값이 바뀔 때만) 요청 DTO 상한은 이 값 */
        const val DESCRIPTION_COLUMN_LENGTH = 255
    }
}
