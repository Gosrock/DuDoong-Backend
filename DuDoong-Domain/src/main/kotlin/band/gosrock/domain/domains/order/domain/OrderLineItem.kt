package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.domain.CartLineItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

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

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
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

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var orderOptionAnswer: List<OrderOptionAnswer> = emptyList()
        private var quantity: Long? = null
        private var orderItemVo: OrderItemVo? = null
        fun orderOptionAnswer(orderOptionAnswer: List<OrderOptionAnswer>) = apply { this.orderOptionAnswer = orderOptionAnswer }
        fun quantity(quantity: Long) = apply { this.quantity = quantity }
        fun orderItemVo(orderItemVo: OrderItemVo) = apply { this.orderItemVo = orderItemVo }
        fun build(): OrderLineItem = OrderLineItem().apply {
            this.orderOptionAnswers.addAll(this@Builder.orderOptionAnswer)
            this.quantity = this@Builder.quantity
            this.orderItem = this@Builder.orderItemVo
        }
    }

    fun getOptionAnswersPrice(): Money =
        orderOptionAnswers.stream()
            .map { it.additionalPrice }
            .reduce(Money.ZERO, Money::plus)

    fun getTotalOrderLinePrice(): Money =
        getItemPrice().plus(getOptionAnswersPrice()).times(quantity!!.toDouble())

    fun getItemPrice(): Money = orderItem!!.price!!

    fun isNeedPaid(): Boolean = Money.ZERO.isLessThan(getTotalOrderLinePrice())

    fun getItemId(): Long = orderItem!!.itemId!!

    fun getItemGroupId(): Long = orderItem!!.itemGroupId!!

    fun getItemName(): String = orderItem!!.name!!

    fun getAnswerOptionIds(): List<Long> =
        orderOptionAnswers.map { it.optionId!! }
}
