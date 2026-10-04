package band.gosrock.domain.domains.order.domain

import band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.order.CreateOrderEvent
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.common.events.order.RefundCompletedOrderEvent
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.domain.Cart
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.order.exception.InvalidOrderException
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException
import band.gosrock.domain.domains.order.exception.OrderLineNotFountException
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkOrderInfo
import band.gosrock.infrastructure.config.mail.dto.EmailOrderInfo
import java.time.LocalDateTime
import java.util.UUID
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
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PostPersist
import jakarta.persistence.PrePersist
import jakarta.persistence.Table

/**
 * (event_id, order_status): v2 공연별 주문 목록·상태별 건수·대시보드 (#712, V004)
 * uuid unique: 주문 조회(승인·거절·취소·상세, v1/v2 공통)의 단건 조회 (#712, V004)
 * (user_id, order_id): 내 주문 목록(v1 마이페이지·v2 O-2, 최신 순)·v2 중복 주문 확인 (#718, V007)
 */
@Table(
    indexes = [
        Index(name = "idx_order_event_id_status", columnList = "event_id, order_status"),
        Index(name = "uk_order_uuid", columnList = "uuid", unique = true),
        Index(name = "idx_order_user_id_id", columnList = "user_id, order_id"),
    ],
)
@Entity(name = "tbl_order")
class Order() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    var id: Long? = null
        protected set

    @Column(nullable = false)
    var userId: Long? = null
        protected set

    @Column(nullable = false)
    var eventId: Long? = null
        protected set

    @Column(nullable = false)
    var uuid: String? = null
        protected set

    var orderNo: String? = null
        protected set

    @Column(nullable = false)
    var orderName: String? = null
        protected set

    @Embedded
    var pgPaymentInfo: PgPaymentInfo = PgPaymentInfo.empty()
        protected set

    var approvedAt: LocalDateTime? = null
        protected set

    var withDrawAt: LocalDateTime? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var orderMethod: OrderMethod? = null
        protected set

    @Embedded
    var totalPaymentInfo: PaymentInfo? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var orderStatus: OrderStatus = OrderStatus.READY
        protected set

    @Embedded
    var orderCouponVo: OrderCouponVo = OrderCouponVo.empty()
        protected set

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var orderLineItems: MutableList<OrderLineItem> = mutableListOf()
        protected set

    @Column(length = 500)
    var failReason: String? = null
        protected set

    @Column(length = 500)
    var cancelReason: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    var refundStatus: RefundStatus = RefundStatus.NONE
        protected set

    var refundStatusChangedAt: LocalDateTime? = null
        protected set

    /** 거절 사유 종류 (v2, #712). v1 거절·취소 주문은 null. 표시 문구는 [cancelReason] 에 함께 기록된다 */
    @Enumerated(EnumType.STRING)
    @Column(name = "refuse_reason_type", length = 30)
    var refuseReasonType: OrderRefuseReasonType? = null
        protected set

    /** 입금자명 (v2 두둥티켓 주문, #718). 주문 시점 값을 저장한다 (닉네임 변경과 무관, DEC-022). v1 주문·무료 주문은 null */
    @Column(name = "depositor_name", length = 20)
    var depositorName: String? = null
        protected set

    /** 결제 방식 (v2 사용자 주문, #718). v1 주문은 null */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", length = 20)
    var paymentChannel: OrderPaymentChannel? = null
        protected set

    @PrePersist
    fun addUUID() {
        uuid = UUID.randomUUID().toString()
    }

    @PostPersist
    fun createOrder() {
        orderNo = "R" + (NO_START_NUMBER + id!!)
        Events.raise(CreateOrderEvent.from(this))
    }

    companion object {
        @JvmStatic
        fun createPaymentOrder(userId: Long, cart: Cart, item: TicketItem, orderValidator: OrderValidator): Order {
            val order = Order().apply {
                this.userId = userId
                this.orderName = cart.cartName
                this.orderLineItems.addAll(getOrderLineItems(cart, item))
                this.orderStatus = OrderStatus.PENDING_PAYMENT
                this.orderMethod = OrderMethod.PAYMENT
                this.eventId = item.eventId
            }
            orderValidator.validCanCreate(order)
            order.calculatePaymentInfo()
            return order
        }

        @JvmStatic
        fun createApproveOrder(userId: Long, cart: Cart, item: TicketItem, orderValidator: OrderValidator): Order {
            val order = Order().apply {
                this.userId = userId
                this.orderName = cart.cartName
                this.orderLineItems.addAll(getOrderLineItems(cart, item))
                this.orderStatus = OrderStatus.PENDING_APPROVE
                this.orderMethod = OrderMethod.APPROVAL
                this.eventId = item.eventId
            }
            orderValidator.validCanCreate(order)
            orderValidator.validApproveStatePurchaseLimit(order)
            orderValidator.validApproveOrderCreateTotalStock(order)
            order.calculatePaymentInfo()
            return order
        }

        @JvmStatic
        fun createPaymentOrderWithCoupon(
            userId: Long,
            cart: Cart,
            item: TicketItem,
            coupon: IssuedCoupon,
            orderValidator: OrderValidator,
        ): Order {
            if (!item.isFCFS() || !cart.isNeedPaid()) {
                throw InvalidOrderException.EXCEPTION
            }
            val supplyAmount = cart.getTotalPrice()
            val couponVo = OrderCouponVo.of(coupon, supplyAmount)
            couponVo.validMinimumPaymentAmount(supplyAmount)
            val order = createPaymentOrder(userId, cart, item, orderValidator)
            order.attachCoupon(couponVo)
            order.calculatePaymentInfo()
            return order
        }

        private fun getOrderLineItems(cart: Cart, item: TicketItem): List<OrderLineItem> =
            cart.cartLineItems.map { OrderLineItem.of(it, item) }

        /** 테스트 전용 팩토리 메서드 */
        @JvmStatic
        @JvmOverloads
        fun forTest(
            userId: Long? = null,
            orderName: String? = null,
            orderLineItems: List<OrderLineItem> = emptyList(),
            orderStatus: OrderStatus = OrderStatus.READY,
            orderMethod: OrderMethod? = null,
            eventId: Long? = null,
            failReason: String? = null,
            cancelReason: String? = null,
            refundStatus: RefundStatus = RefundStatus.NONE,
        ): Order = Order().apply {
            this.userId = userId
            this.orderName = orderName
            this.orderLineItems.addAll(orderLineItems)
            this.orderStatus = orderStatus
            this.orderMethod = orderMethod
            this.eventId = eventId
            this.failReason = failReason
            this.cancelReason = cancelReason
            this.refundStatus = refundStatus
        }
    }

    fun calculatePaymentInfo() {
        totalPaymentInfo = PaymentInfo.of(
            discountAmount = getTotalDiscountPrice(),
            paymentAmount = getTotalPaymentPrice(),
            supplyAmount = getTotalSupplyPrice(),
        )
    }

    fun confirmPayment(approvedAt: LocalDateTime, pgPaymentInfo: PgPaymentInfo, orderValidator: OrderValidator) {
        issueDoneOrderEvent()
        orderValidator.validCanConfirmPayment(this)
        orderStatus = OrderStatus.CONFIRM
        this.approvedAt = approvedAt
        this.pgPaymentInfo = pgPaymentInfo
    }

    fun approve(orderValidator: OrderValidator) {
        issueDoneOrderEvent()
        orderValidator.validCanApproveOrder(this)
        approvedAt = LocalDateTime.now()
        orderStatus = OrderStatus.APPROVED
    }

    fun freeConfirm(currentUserId: Long, orderValidator: OrderValidator) {
        orderValidator.validOwner(this, currentUserId)
        issueDoneOrderEvent()
        orderValidator.validCanFreeConfirm(this)
        approvedAt = LocalDateTime.now()
        orderStatus = OrderStatus.APPROVED
    }

    private fun issueDoneOrderEvent() {
        if (orderStatus.isCanDone()) {
            Events.raise(DoneOrderEvent.from(this))
        }
    }

    fun cancel(orderValidator: OrderValidator, reason: String? = null) {
        orderValidator.validCanCancel(this)
        orderStatus = OrderStatus.CANCELED
        cancelReason = reason?.take(500)
        refundStatus = RefundStatus.REFUND_REQUESTED
        refundStatusChangedAt = LocalDateTime.now()
        withDrawAt = LocalDateTime.now()
        Events.raise(WithDrawOrderEvent.from(this))
    }

    fun refuse(orderValidator: OrderValidator, reason: String? = null) {
        orderValidator.validCanRefuse(this)
        orderStatus = OrderStatus.CANCELED
        cancelReason = reason?.take(500)
        refundStatus = RefundStatus.REFUND_REQUESTED
        refundStatusChangedAt = LocalDateTime.now()
        withDrawAt = LocalDateTime.now()
        Events.raise(WithDrawOrderEvent.from(this))
    }

    fun refund(currentUserId: Long, orderValidator: OrderValidator, reason: String? = null) {
        orderValidator.validOwner(this, currentUserId)
        orderValidator.validCanRefund(this)
        orderStatus = OrderStatus.REFUND
        cancelReason = reason?.take(500)
        refundStatus = RefundStatus.REFUND_REQUESTED
        refundStatusChangedAt = LocalDateTime.now()
        withDrawAt = LocalDateTime.now()
        Events.raise(WithDrawOrderEvent.from(this))
    }

    fun fail(reason: String? = null) {
        orderStatus = OrderStatus.FAILED
        failReason = reason?.take(500)
    }

    fun completeRefund() {
        refundStatus = RefundStatus.REFUND_COMPLETED
        refundStatusChangedAt = LocalDateTime.now()
        // 저장 전 주문(uuid 없음)은 알림 대상이 아니다
        uuid?.let { Events.raise(RefundCompletedOrderEvent(it, orderMethod)) }
    }

    fun attachCoupon(orderCouponVo: OrderCouponVo) {
        this.orderCouponVo = orderCouponVo
    }

    val paymentKey: String
        get() = pgPaymentInfo.paymentKey.takeIf { it.isNotEmpty() }
            ?: throw NotPaymentOrderException.EXCEPTION

    fun getCouponName(): String = orderCouponVo.name

    fun getTotalSupplyPrice(): Money =
        orderLineItems.fold(Money.ZERO) { acc, item -> acc.plus(item.getTotalOrderLinePrice()) }

    fun getTotalPaymentPrice(): Money = getTotalSupplyPrice().minus(getTotalDiscountPrice())

    fun getTotalDiscountPrice(): Money = orderCouponVo.discountAmount

    fun hasCoupon(): Boolean = !orderCouponVo.isDefault()

    private fun getOrderLineItem(): OrderLineItem =
        orderLineItems.firstOrNull() ?: throw OrderLineNotFountException.EXCEPTION

    val itemId: Long
        get() = getOrderLineItem().getItemId()

    fun getItemGroupId(): Long = getOrderLineItem().getItemGroupId()

    fun isNeedPaid(): Boolean =
        Money.ZERO.isLessThan(getTotalPaymentPrice()) && orderMethod!!.isPayment()

    fun getMethod(): String {
        if (orderMethod == OrderMethod.APPROVAL) return OrderMethod.APPROVAL.kr
        return pgPaymentInfo.paymentMethod.kr
    }

    fun getProvider(): String = pgPaymentInfo.paymentProvider

    fun getReceiptUrl(): String = pgPaymentInfo.receiptUrl

    fun isPaid(): Boolean = isNeedPaid()

    fun isDudoongTicketOrder(): Boolean =
        getTotalPaymentPrice().isGreaterThan(Money.ZERO) && orderMethod == OrderMethod.APPROVAL

    fun getDistinctItemIds(): List<Long> =
        orderLineItems.map { it.getItemId() }.distinct()

    fun getTotalQuantity(): Long =
        orderLineItems.sumOf { it.quantity!! }

    fun toEmailOrderInfo(): EmailOrderInfo =
        EmailOrderInfo(orderName!!, getTotalQuantity(), getTotalPaymentPrice().toString(), createdAtKt())

    fun toAlimTalkOrderInfo(): AlimTalkOrderInfo =
        AlimTalkOrderInfo(orderName!!, getTotalQuantity(), getTotalPaymentPrice().toString(), createdAtKt())

    // ===== v2 공유 데이터 (검증·조합 규칙은 service.v2.V2OrderDomainService) =====

    /** 거절 사유 종류 기록. [refuse] 와 같은 트랜잭션·락 안에서 V2OrderDomainService 가 호출한다 */
    internal fun recordRefuseReasonType(type: OrderRefuseReasonType) {
        this.refuseReasonType = type
    }

    /** v2 결제 방식·입금자명 기록. 저장 전(같은 트랜잭션·락) V2UserOrderDomainService 가 호출한다 */
    internal fun recordV2Payment(channel: OrderPaymentChannel, depositorName: String?) {
        this.paymentChannel = channel
        this.depositorName = depositorName
    }

    /**
     * v2 사용자 취소 (#718): 승인 대기 주문, 또는 환불할 돈이 없는 승인 주문(무료).
     * 상태는 v1 사용자 환불([refund])과 같은 REFUND 로 남겨 v1 화면·메일·슬랙이 '구매자 환불'로 처리하게 하고,
     * 환불 요청(REFUND_REQUESTED)은 [refundRequested] 일 때만 건다. 검증(본인·상태·기한·입장 여부)은 V2UserOrderDomainService 가 같은 락 안에서 한다
     */
    internal fun withdrawByUser(refundRequested: Boolean) {
        val now = LocalDateTime.now()
        orderStatus = OrderStatus.REFUND
        if (refundRequested) {
            refundStatus = RefundStatus.REFUND_REQUESTED
            refundStatusChangedAt = now
        }
        withDrawAt = now
        Events.raise(WithDrawOrderEvent.from(this))
    }
}
