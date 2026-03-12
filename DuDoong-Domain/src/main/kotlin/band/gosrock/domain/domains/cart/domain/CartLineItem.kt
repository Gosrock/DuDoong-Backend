package band.gosrock.domain.domains.cart.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

@Entity(name = "tbl_cart_line_item")
class CartLineItem() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_line_id")
    var id: Long? = null
        protected set

    @Column(nullable = false)
    var itemId: Long? = null
        protected set

    @Column(nullable = false)
    var itemPrice: Money? = null
        protected set

    @Column(nullable = false)
    var quantity: Long? = null
        protected set

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_line_id")
    var cartOptionAnswers: MutableList<CartOptionAnswer> = mutableListOf()
        protected set

    companion object {
        @JvmStatic
        fun of(item: TicketItem, quantity: Long, cartOptionAnswers: List<CartOptionAnswer>): CartLineItem =
            CartLineItem().apply {
                this.itemId = item.id
                this.itemPrice = item.price
                this.quantity = quantity
                this.cartOptionAnswers.addAll(cartOptionAnswers)
            }

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var item: TicketItem? = null
        private var quantity: Long? = null
        private var cartOptionAnswers: List<CartOptionAnswer> = emptyList()
        fun item(item: TicketItem) = apply { this.item = item }
        fun quantity(quantity: Long) = apply { this.quantity = quantity }
        fun cartOptionAnswers(cartOptionAnswers: List<CartOptionAnswer>) = apply { this.cartOptionAnswers = cartOptionAnswers }
        fun build(): CartLineItem = of(item!!, quantity!!, cartOptionAnswers)
    }

    fun getTotalOptionsPrice(): Money =
        cartOptionAnswers.stream()
            .map { it.additionalPrice }
            .reduce(Money.ZERO, Money::plus)

    fun getTotalCartLinePrice(): Money =
        itemPrice!!.plus(getTotalOptionsPrice()).times(quantity!!.toDouble())

    fun isNeedPaid(): Boolean = Money.ZERO.isLessThan(getTotalCartLinePrice())

    fun getAnswerOptionIds(): List<Long> =
        cartOptionAnswers.map { it.optionId!! }
}
