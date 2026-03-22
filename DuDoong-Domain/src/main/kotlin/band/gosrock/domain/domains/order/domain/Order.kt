package band.gosrock.domain.domains.order.domain

import band.gosrock.common.consts.DuDoongStatic.NO_START_NUMBER
import band.gosrock.domain.common.aop.domainEvent.Events
import band.gosrock.domain.common.events.order.CreateOrderEvent
import band.gosrock.domain.common.events.order.DoneOrderEvent
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
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PostPersist
import jakarta.persistence.PrePersist

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
        fun forTest(
            userId: Long? = null,
            orderName: String? = null,
            orderLineItems: List<OrderLineItem> = emptyList(),
            orderStatus: OrderStatus = OrderStatus.READY,
            orderMethod: OrderMethod? = null,
            eventId: Long? = null,
        ): Order = Order().apply {
            this.userId = userId
            this.orderName = orderName
            this.orderLineItems.addAll(orderLineItems)
            this.orderStatus = orderStatus
            this.orderMethod = orderMethod
            this.eventId = eventId
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

    fun cancel(orderValidator: OrderValidator) {
        orderValidator.validCanCancel(this)
        orderStatus = OrderStatus.CANCELED
        withDrawAt = LocalDateTime.now()
        Events.raise(WithDrawOrderEvent.from(this))
    }

    fun refuse(orderValidator: OrderValidator) {
        orderValidator.validCanRefuse(this)
        orderStatus = OrderStatus.CANCELED
        withDrawAt = LocalDateTime.now()
        Events.raise(WithDrawOrderEvent.from(this))
    }

    fun refund(currentUserId: Long, orderValidator: OrderValidator) {
        orderValidator.validOwner(this, currentUserId)
        orderValidator.validCanRefund(this)
        orderStatus = OrderStatus.REFUND
        withDrawAt = LocalDateTime.now()
        Events.raise(WithDrawOrderEvent.from(this))
    }

    fun fail() {
        orderStatus = OrderStatus.FAILED
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
}
