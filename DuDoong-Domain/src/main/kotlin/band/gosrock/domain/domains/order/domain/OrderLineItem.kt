package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.domain.CartLineItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany

@Entity(name = "tbl_order_line")
class OrderLineItem() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_line_item_id")
    var id: Long? = null
        protected set

    @Embedded
    var orderItem: OrderItemVo? = null
        protected set

    var quantity: Long? = null
        protected set

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "order_line_item_id")
    var orderOptionAnswers: MutableList<OrderOptionAnswer> = mutableListOf()
        protected set

    companion object {
        @JvmStatic
        fun of(cartLineItem: CartLineItem, ticketItem: TicketItem): OrderLineItem {
            val orderOptionAnswers = cartLineItem.cartOptionAnswers.map { OrderOptionAnswer.from(it) }
            return OrderLineItem().apply {
                this.orderOptionAnswers.addAll(orderOptionAnswers)
                this.quantity = cartLineItem.quantity
                this.orderItem = OrderItemVo.from(ticketItem)
            }
        }

        /** 테스트 전용 팩토리 메서드 */
        @JvmStatic
        fun forTest(
            orderOptionAnswer: List<OrderOptionAnswer> = emptyList(),
            quantity: Long? = null,
            orderItemVo: OrderItemVo? = null,
        ): OrderLineItem = OrderLineItem().apply {
            this.orderOptionAnswers.addAll(orderOptionAnswer)
            this.quantity = quantity
            this.orderItem = orderItemVo
        }
    }

    private val safeOrderItem: OrderItemVo
        get() = orderItem ?: throw IllegalStateException("OrderItem is not initialized")

    private val safeQuantity: Long
        get() = quantity ?: throw IllegalStateException("Quantity is not initialized")

    fun getOptionAnswersPrice(): Money =
        orderOptionAnswers.fold(Money.ZERO) { acc, answer -> acc.plus(answer.additionalPrice) }

    fun getTotalOrderLinePrice(): Money =
        getItemPrice().plus(getOptionAnswersPrice()).times(safeQuantity.toDouble())

    fun getItemPrice(): Money = safeOrderItem.price ?: throw IllegalStateException("OrderItem price is not set")

    fun isNeedPaid(): Boolean = Money.ZERO.isLessThan(getTotalOrderLinePrice())

    fun getItemId(): Long = safeOrderItem.itemId ?: throw IllegalStateException("OrderItem itemId is not set")

    fun getItemGroupId(): Long = safeOrderItem.itemGroupId ?: throw IllegalStateException("OrderItem itemGroupId is not set")

    fun getItemName(): String = safeOrderItem.name ?: throw IllegalStateException("OrderItem name is not set")

    fun getAnswerOptionIds(): List<Long> =
        orderOptionAnswers.map { it.optionId ?: throw IllegalStateException("OrderOptionAnswer optionId is not set") }
}
