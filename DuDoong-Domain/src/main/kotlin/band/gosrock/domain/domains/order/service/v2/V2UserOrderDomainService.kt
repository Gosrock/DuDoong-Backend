package band.gosrock.domain.domains.order.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.common.consts.DuDoongStatic.KR_NO
import band.gosrock.common.consts.DuDoongStatic.KR_YES
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.aop.redissonLock.LockNames
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.domain.Cart
import band.gosrock.domain.domains.cart.domain.CartLineItem
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer
import band.gosrock.domain.domains.cart.domain.CartValidator
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.EventNotOpenException
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.order.exception.CanNotCancelOrderException
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException
import band.gosrock.domain.domains.order.exception.OrderNotFoundException
import band.gosrock.domain.domains.order.exception.V2DuplicateOrderInProgressException
import band.gosrock.domain.domains.order.exception.V2InvalidDepositorNameException
import band.gosrock.domain.domains.order.exception.V2InvalidOptionAnswersException
import band.gosrock.domain.domains.order.exception.V2InvalidPaymentMethodException
import band.gosrock.domain.domains.order.exception.V2OrderCannotCancelByUserException
import band.gosrock.domain.domains.order.exception.V2RefundAccountNotAllowedException
import band.gosrock.domain.domains.order.exception.V2RefundAccountRequiredException
import band.gosrock.domain.domains.order.exception.V2RefundAlreadyCompletedException
import band.gosrock.domain.domains.order.exception.V2UnsupportedOrderTicketException
import band.gosrock.domain.domains.order.repository.OrderRefundAccountRepository
import band.gosrock.domain.domains.order.service.OrderFactory
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import band.gosrock.domain.domains.ticket_item.domain.OptionGroupType
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

/** 옵션 답변 하나. [optionId] 는 v2 옵션 id (= v1 옵션 그룹 id) */
data class V2OrderAnswerForm(val optionId: Long, val answer: String?)

/**
 * v2 사용자 주문 생성 입력 (O-1).
 * @property answerSets 티켓 묶음별 답변. applyToAll 이면 1개(수량 전체에 적용), 티켓별이면 수량과 같은 개수(티켓 1장씩)
 */
data class V2CreateOrderCommand(
    val userId: Long,
    val eventId: Long,
    val ticketItemId: Long,
    val quantity: Long,
    val applyToAll: Boolean,
    val answerSets: List<List<V2OrderAnswerForm>>,
    val paymentChannel: OrderPaymentChannel,
    val depositorName: String?,
)

/** 생성 결과. [needsFreeConfirm] 이면 호출 측이 v1 무료 확정(`FreeOrderService`)을 이어서 부른다 */
data class V2CreatedOrder(val orderUuid: String, val needsFreeConfirm: Boolean, val duplicated: Boolean)

data class V2RefundAccountForm(val bankName: String, val accountHolder: String, val accountNumber: String)

/**
 * v2 사용자 앱 주문 규칙 (#718, DEC-018·DEC-022). v1 코드는 이 서비스를 호출하지 않는다.
 *
 * 생성(O-1): v1 장바구니 → 주문 단계를 한 트랜잭션에서 한다. 장바구니는 **저장하지 않는다**(메모리 객체로 v1 [CartValidator] 검증만) —
 * v1 앱의 '최근 장바구니'(사용자당 1개, 만들 때 이전 것을 지움)를 덮어쓰지 않기 위해서다. 주문은 v1 [OrderFactory] 규칙 그대로
 * (두둥 → 승인 대기, 무료 승인 → 승인 대기, 무료 선착순 → 결제형 생성 후 v1 무료 확정).
 * 티켓별 옵션 답변은 같은 티켓의 **수량 1짜리 주문 라인 N개**로 표현한다 (v1 장바구니가 원래 지원하는 구조, 발급 시 라인 답변이 티켓으로 복사됨).
 *
 * 취소(O-4): 승인 대기는 공연 시작 전 언제든, 승인 완료는 공연 시작 전 + 입장·선물 대기·선물 완료 티켓 없음(#719). 상태는 v1 사용자 환불과 같은 REFUND.
 */
@DomainService
@Transactional(readOnly = true)
class V2UserOrderDomainService(
    private val orderAdaptor: OrderAdaptor,
    private val orderValidator: OrderValidator,
    private val orderFactory: OrderFactory,
    private val cartValidator: CartValidator,
    private val eventAdaptor: EventAdaptor,
    private val refundAccountRepository: OrderRefundAccountRepository,
    private val v2TicketItemDomainService: V2TicketItemDomainService,
    private val v2UserOrderQuery: V2UserOrderQuery,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val entityManager: EntityManager,
) {

    /**
     * 주문 생성. `티켓관리:{ticketItemId}` 락(v1 발급·재고 감소, v2 티켓 수정과 같은 락)으로 같은 티켓 주문을 줄 세운다:
     * 승인 대기 재고·1인 제한 검사가 동시 주문에도 맞고, 판매 조건(가격·계좌·옵션)이 바뀌는 중에 주문이 끼어들지 않는다.
     * 같은 사용자의 같은 요청이 [DUPLICATE_WINDOW_SECONDS]초 안에 다시 오면 앞 주문을 돌려준다(중복 클릭·재전송).
     */
    @RedissonLock(LockName = TICKET_LOCK, identifier = "ticketItemId")
    fun create(ticketItemId: Long, command: V2CreateOrderCommand): V2CreatedOrder {
        val item = v2TicketItemDomainService.queryTicketItem(command.eventId, ticketItemId)
        if (item.payType !in V2TicketItemDomainService.V2_ORDER_PAY_TYPES) throw V2UnsupportedOrderTicketException.EXCEPTION
        val depositorName = validatePayment(item, command.paymentChannel, command.depositorName)

        val lines = cartLines(item, command)
        findDuplicate(command, depositorName, lines)?.let { return it }

        val cart = Cart.of(lines, item.name!!, command.userId, cartValidator)
        if (item.payType == TicketPayType.FREE_TICKET && item.isFCFS()) validateUnconfirmedPurchaseLimit(item, command)
        val order = orderFactory.createNormalOrder(cart, command.userId)
        order.recordV2Payment(command.paymentChannel, depositorName)
        val saved = orderAdaptor.save(order)
        return V2CreatedOrder(saved.uuid!!, needsFreeConfirm = saved.orderStatus == OrderStatus.PENDING_PAYMENT, duplicated = false)
    }

    /**
     * 무료 선착순 1인 제한 보강: v1 검사(발급 수 + 이번 수량)에 확정 전 v2 주문 수량을 더한다.
     * 같은 티켓 락 안이라 동시 요청(옵션만 다른 두 요청 등)도 앞 주문이 보인다. 확정 실패 주문은 FAILED 로 바뀌어 빠지고,
     * 서버 중단 등으로 확정 전 상태로 남은 주문은 [UNCONFIRMED_WINDOW_MINUTES]분이 지나면 세지 않는다 (영구 차단 방지)
     */
    private fun validateUnconfirmedPurchaseLimit(item: TicketItem, command: V2CreateOrderCommand) {
        val since = LocalDateTime.now().minusMinutes(UNCONFIRMED_WINDOW_MINUTES)
        val issued = issuedTicketAdaptor.countPaidTicket(command.userId, item.id!!)
        val unconfirmed = v2UserOrderQuery.sumUnconfirmedV2Quantity(command.userId, item.id!!, since)
        item.validPurchaseLimit(issued + unconfirmed + command.quantity)
    }

    /**
     * 무료 확정 실패 처리 (O-1): 확정 전(PENDING_PAYMENT) 그대로면 FAILED 로 바꾼다 — 중복 요청 판정·1인 제한에서 빠져 재시도가 새 주문으로 정상 진행된다.
     * 확정 트랜잭션은 이미 롤백됐으므로 `주문` 락의 새 트랜잭션에서 한다
     */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun failUnconfirmed(orderUuid: String, reason: String?) {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.orderStatus == OrderStatus.PENDING_PAYMENT) order.fail(reason)
    }

    /** 결제 방식·입금자명 검증. 두둥티켓은 계좌이체/토스 + 입금자명 1~20자(앞뒤 공백 제외), 무료는 FREE (입금자명은 저장 안 함) */
    fun validatePayment(item: TicketItem, channel: OrderPaymentChannel, depositorName: String?): String? =
        when (item.payType) {
            TicketPayType.DUDOONG_TICKET -> {
                if (!channel.isTransfer()) throw V2InvalidPaymentMethodException.EXCEPTION
                val name = depositorName?.trim().orEmpty()
                if (name.isEmpty() || name.length > DEPOSITOR_NAME_MAX_LENGTH) throw V2InvalidDepositorNameException.EXCEPTION
                name
            }
            TicketPayType.FREE_TICKET -> {
                if (channel != OrderPaymentChannel.FREE) throw V2InvalidPaymentMethodException.EXCEPTION
                null
            }
            else -> throw V2UnsupportedOrderTicketException.EXCEPTION
        }

    /**
     * 같은 요청(티켓·라인별 수량·옵션 답변·결제 방식·입금자명이 모두 같음)으로 [DUPLICATE_WINDOW_SECONDS]초 안에 만든 진행 중·완료 주문.
     * 거절·취소·실패한 주문은 보지 않는다 (다시 주문하는 것은 정상)
     */
    private fun findDuplicate(command: V2CreateOrderCommand, depositorName: String?, lines: List<CartLineItem>): V2CreatedOrder? {
        val since = LocalDateTime.now().minusSeconds(DUPLICATE_WINDOW_SECONDS)
        val requested = lines.map { line -> line.quantity!! to line.cartOptionAnswers.map { it.optionId to it.answer }.sortedBy { it.first } }
        val same = v2UserOrderQuery.findRecentOrders(command.userId, command.ticketItemId, since, DUPLICATE_CANDIDATE_STATUSES).firstOrNull {
            it.paymentChannel == command.paymentChannel && it.depositorName == depositorName && it.lines == requested
        } ?: return null
        // 무료 선착순 주문이 아직 확정(발급) 전이면 앞 요청이 확정 중이다. 여기서 또 확정하면 이중 발급 위험이 있어 돌려보낸다
        if (same.orderStatus == OrderStatus.PENDING_PAYMENT || same.orderStatus == OrderStatus.READY) {
            throw V2DuplicateOrderInProgressException.EXCEPTION
        }
        return V2CreatedOrder(same.orderUuid, needsFreeConfirm = false, duplicated = true)
    }

    /** 답변 묶음 → v1 장바구니 라인. 일괄이면 라인 1개(수량 전체), 티켓별이면 수량 1 라인 N개 */
    private fun cartLines(item: TicketItem, command: V2CreateOrderCommand): List<CartLineItem> {
        val groups = item.itemOptionGroups.mapNotNull { it.optionGroup }.associateBy { it.id!! }
        if (command.applyToAll) {
            if (command.answerSets.size != 1) throw V2InvalidOptionAnswersException.EXCEPTION
            return listOf(CartLineItem.of(item, command.quantity, cartAnswers(groups, command.answerSets[0])))
        }
        if (command.answerSets.size.toLong() != command.quantity) throw V2InvalidOptionAnswersException.EXCEPTION
        return command.answerSets.map { CartLineItem.of(item, 1L, cartAnswers(groups, it)) }
    }

    /** 티켓에 붙은 옵션 전부에 한 번씩 답해야 한다. 네/아니오는 YES·NO(또는 예·네 / 아니요·아니오), 주관식은 1~255자 */
    private fun cartAnswers(groups: Map<Long, OptionGroup>, answers: List<V2OrderAnswerForm>): List<CartOptionAnswer> {
        if (answers.map { it.optionId }.toSet() != groups.keys || answers.size != groups.size) {
            throw V2InvalidOptionAnswersException.EXCEPTION
        }
        return answers.map { form ->
            val group = groups.getValue(form.optionId)
            val answer = form.answer?.trim().orEmpty()
            when (group.type) {
                OptionGroupType.TRUE_FALSE -> {
                    val label = when (answer.uppercase()) {
                        in YES_ANSWERS -> KR_YES
                        in NO_ANSWERS -> KR_NO
                        else -> throw V2InvalidOptionAnswersException.EXCEPTION
                    }
                    val row = group.options.firstOrNull { it.answer == label } ?: throw V2InvalidOptionAnswersException.EXCEPTION
                    CartOptionAnswer.of(row, label)
                }
                OptionGroupType.SUBJECTIVE -> {
                    if (answer.isEmpty() || answer.length > SUBJECTIVE_ANSWER_MAX_LENGTH) throw V2InvalidOptionAnswersException.EXCEPTION
                    val row = group.options.singleOrNull() ?: throw V2InvalidOptionAnswersException.EXCEPTION
                    CartOptionAnswer.of(row, answer)
                }
                else -> throw V2InvalidOptionAnswersException.EXCEPTION
            }
        }
    }

    // ===== 조회 =====

    /** 본인 주문. 남의 주문·없는 주문은 존재를 드러내지 않도록 404 */
    fun queryMyOrder(userId: Long, orderUuid: String): Order {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        if (order.userId != userId) throw OrderNotFoundException.EXCEPTION
        return order
    }

    fun refundAccountOf(orderId: Long): OrderRefundAccount? = refundAccountRepository.findByOrderId(orderId)

    fun refundAccountsOf(orderIds: Collection<Long>): Map<Long, OrderRefundAccount> =
        if (orderIds.isEmpty()) emptyMap()
        else refundAccountRepository.findByOrderIdIn(orderIds.toSet()).associateBy { it.orderId }

    /** 지금 사용자가 취소할 수 있는지 (O-3 canCancel). [cancel] 과 같은 판정 */
    fun canCancel(order: Order, event: Event, now: LocalDateTime = LocalDateTime.now()): Boolean =
        cancelBlocker(order, event, now) == null

    // ===== 취소 =====

    /**
     * 사용자 취소·환불 요청 (O-4). `주문:{uuid}` 락(v1 승인·거절·취소·환불과 같은 락) 안에서 판정·전이한다.
     * - 승인 완료 + 유료: v1 사용자 환불과 같은 [Order.refund] (REFUND + 환불 요청, 발급 티켓 취소·재고 복구는 v1 핸들러)
     * - 승인 대기, 무료 승인: [Order.withdrawByUser] (REFUND, 유료면 환불 요청 — 이미 입금했을 수 있음)
     * - 유료면 환불 계좌 필수, 함께 저장
     */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun cancel(userId: Long, orderUuid: String, refundAccount: V2RefundAccountForm?) {
        val order = queryMyOrder(userId, orderUuid)
        val event = eventAdaptor.findById(order.eventId!!)
        cancelBlocker(order, event, LocalDateTime.now())?.let { throw it }
        val paid = isPaid(order)
        val account = refundAccount?.let { normalize(it) }
        if (paid && account == null) throw V2RefundAccountRequiredException.EXCEPTION

        if (order.orderStatus != OrderStatus.PENDING_APPROVE && paid) {
            order.refund(userId, orderValidator)
        } else {
            order.withdrawByUser(refundRequested = paid)
        }
        if (paid && refundAccountRepository.findByOrderId(order.id!!) == null) {
            refundAccountRepository.save(
                OrderRefundAccount(orderId = order.id!!, bankName = account!!.bankName, accountHolder = account.accountHolder, accountNumber = account.accountNumber),
            )
        }
    }

    // ===== 환불 계좌 입력·수정 (#728) =====

    /**
     * 환불 계좌를 받을 수 있는 주문인지 (호스트 거절·호스트 취소·사용자 취소 공통): v2 주문 + 유료 + 계좌이체(승인형, 두둥티켓) + 환불 요청 중.
     * v1 주문(결제 채널 없음)은 오래된 주문에 입력을 유도하지 않도록 대상이 아니다 (사용자 결정 2026-10-05). 단 v2 앱에서 O-4 로 취소하며
     * 계좌를 입력해 계좌 행이 이미 있는 v1 주문은 오타를 고칠 수 있게 **수정만** 허용한다 (#728 재리뷰 — 계좌가 있으므로 입력 필요는 항상 false).
     * 카드(PG) 결제는 결제 취소가 자동이라, 무료·0원은 돌려줄 돈이 없어 대상이 아니다. 순서: 대상 아님(Order_400_27) → 대상이지만 환불 완료(Order_400_28)
     */
    fun refundAccountBlocker(order: Order): DuDoongCodeException? = when {
        !isV2OrHasRefundAccount(order) || order.orderMethod != OrderMethod.APPROVAL || !isPaid(order) ||
            order.orderStatus !in REFUND_ACCOUNT_ORDER_STATUSES || order.refundStatus !in REFUND_ACCOUNT_REFUND_STATUSES ->
            V2RefundAccountNotAllowedException.EXCEPTION
        order.refundStatus == RefundStatus.REFUND_COMPLETED -> V2RefundAlreadyCompletedException.EXCEPTION
        else -> null
    }

    /** O-3 `canEditRefundAccount`: 지금 환불 계좌를 입력·수정할 수 있는지 */
    fun canEditRefundAccount(order: Order): Boolean = refundAccountBlocker(order) == null

    /**
     * O-3 `refundAccountRequired`·거절/호스트 취소 알림의 입력 안내: 입력할 수 있고 아직 계좌가 없을 때.
     * 입금 미확인 거절은 돌려줄 돈이 없을 가능성이 커서 요구하지 않는다 (입력·수정은 가능, 사용자 결정 2026-10-05)
     */
    fun isRefundAccountRequired(order: Order, account: OrderRefundAccount?): Boolean =
        account == null && order.refuseReasonType != OrderRefuseReasonType.DEPOSIT_UNCONFIRMED && canEditRefundAccount(order)

    /**
     * 환불 계좌 입력·수정 (#728, 사용자 결정 2026-10-05). 본인 주문만(남의·없는 주문 404).
     * `주문:{uuid}` 락(O-4 와 같은 락) + 주문 행 잠금 다시 읽기: v1·운영의 환불 완료는 주문 락 없이 주문 행만 UPDATE 하므로 행 잠금으로 줄 서고,
     * 완료가 먼저 커밋됐으면 최신 상태를 보고 Order_400_28. 이미 읽혀 영속성 컨텍스트에 있는 주문이어도 낡은 상태로 판정하지 않도록
     * 잠금 쿼리가 아니라 refresh 로 다시 읽는다. 소유자(userId)는 바뀌지 않는 값이라 잠그기 전에 확인한다 (남의 주문 행은 잠그지 않음).
     * 계좌 정규화·검증은 O-4 와 같다(빈 값 Order_400_25). 이미 있던 계좌를 다른 값으로 바꾸면 호스트 마스터·매니저 알림 이벤트를 낸다 (첫 입력은 알림 없음)
     */
    @RedissonLock(LockName = ORDER_LOCK, identifier = "orderUuid")
    fun putRefundAccount(userId: Long, orderUuid: String, form: V2RefundAccountForm): OrderRefundAccount {
        val order = queryMyOrder(userId, orderUuid)
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE)
        refundAccountBlocker(order)?.let { throw it }
        val account = normalize(form) ?: throw V2RefundAccountRequiredException.EXCEPTION
        val existing = refundAccountRepository.findByOrderId(order.id!!)
        if (existing != null) {
            if (existing.bankName != account.bankName || existing.accountHolder != account.accountHolder || existing.accountNumber != account.accountNumber) {
                existing.change(account.bankName, account.accountHolder, account.accountNumber)
                Events.raise(V2RefundAccountChangedEvent(orderUuid = orderUuid))
            }
            return existing
        }
        return refundAccountRepository.save(
            OrderRefundAccount(orderId = order.id!!, bankName = account.bankName, accountHolder = account.accountHolder, accountNumber = account.accountNumber),
        )
    }

    /** 취소를 막는 사유(예외). 없으면 null. 순서: 결제 방식 → 주문 상태 → 공연 상태·시작 전 → 입장·선물 대기·선물 완료 티켓 */
    private fun cancelBlocker(order: Order, event: Event, now: LocalDateTime): DuDoongCodeException? {
        // 카드(PG) 결제 주문은 v1 결제 취소 경로 (계좌 환불 대상 아님)
        if (order.orderMethod == OrderMethod.PAYMENT && isPaid(order)) return V2OrderCannotCancelByUserException.EXCEPTION
        val pending = order.orderStatus == OrderStatus.PENDING_APPROVE
        if (!pending && !order.orderStatus.isCanWithDraw()) return CanNotCancelOrderException.EXCEPTION
        // v1 철회 검증(validCanWithDraw / validAvailableRefundDate)과 같은 기준: 공연 OPEN + 시작 전
        if (event.status != EventStatus.OPEN) return EventNotOpenException.EXCEPTION
        val startAt = event.getStartAt() ?: return NotRefundAvailableDateOrderException.EXCEPTION
        if (!now.isBefore(startAt)) return NotRefundAvailableDateOrderException.EXCEPTION
        if (!pending && v2UserOrderQuery.countCancelBlockingTickets(order.uuid!!, order.userId!!) > 0) {
            return V2OrderCannotCancelByUserException.EXCEPTION
        }
        return null
    }

    /** v2 주문이거나, v1 주문이지만 O-4 로 입력한 계좌 행이 있음 (v1 주문만 계좌를 조회한다) */
    private fun isV2OrHasRefundAccount(order: Order): Boolean =
        order.paymentChannel != null || order.id?.let { refundAccountRepository.findByOrderId(it) } != null

    private fun isPaid(order: Order): Boolean = order.getTotalPaymentPrice().isGreaterThan(Money.ZERO)

    private fun normalize(form: V2RefundAccountForm): V2RefundAccountForm? {
        val bank = form.bankName.trim()
        val holder = form.accountHolder.trim()
        // 계좌번호는 공백을 모두 지워 저장 (호스트가 그대로 복사해 송금)
        val number = form.accountNumber.filterNot { it.isWhitespace() }
        if (bank.isEmpty() || holder.isEmpty() || number.isEmpty()) return null
        return V2RefundAccountForm(bank, holder, number)
    }

    companion object {
        private const val TICKET_LOCK = LockNames.TICKET
        private const val ORDER_LOCK = "주문"
        const val DEPOSITOR_NAME_MAX_LENGTH = 20
        const val SUBJECTIVE_ANSWER_MAX_LENGTH = 255

        /** 같은 사용자·티켓·수량·결제 방식·입금자명 주문을 중복 요청으로 보는 시간 */
        const val DUPLICATE_WINDOW_SECONDS = 10L

        /** 확정 전 v2 무료 주문을 1인 제한에 세는 기간 (정상 흐름에서는 생성 직후 확정되거나 FAILED 가 된다) */
        const val UNCONFIRMED_WINDOW_MINUTES = 5L

        /** 환불 계좌를 받는 주문 상태: 거절·호스트 취소(CANCELED), 사용자 취소(REFUND) */
        private val REFUND_ACCOUNT_ORDER_STATUSES = setOf(OrderStatus.CANCELED, OrderStatus.REFUND)

        /** 환불 계좌 대상 환불 상태: 요청 중(입력·수정 가능), 완료(Order_400_28) */
        private val REFUND_ACCOUNT_REFUND_STATUSES = setOf(RefundStatus.REFUND_REQUESTED, RefundStatus.REFUND_COMPLETED)

        private val DUPLICATE_CANDIDATE_STATUSES = listOf(
            OrderStatus.READY, OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_APPROVE, OrderStatus.APPROVED, OrderStatus.CONFIRM,
        )

        private val YES_ANSWERS = setOf("YES", KR_YES, "네")
        private val NO_ANSWERS = setOf("NO", KR_NO, "아니오")
    }
}
