package band.gosrock.domain.domains.cart.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany

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
    }

    fun getTotalOptionsPrice(): Money =
        cartOptionAnswers.fold(Money.ZERO) { acc, answer -> acc.plus(answer.additionalPrice) }

    fun getTotalCartLinePrice(): Money {
        val price = itemPrice ?: throw IllegalStateException("CartLineItem itemPrice is not initialized")
        val qty = quantity ?: throw IllegalStateException("CartLineItem quantity is not initialized")
        return price.plus(getTotalOptionsPrice()).times(qty.toDouble())
    }

    fun isNeedPaid(): Boolean = Money.ZERO.isLessThan(getTotalCartLinePrice())

    fun getAnswerOptionIds(): List<Long> =
        cartOptionAnswers.map { it.optionId!! }
}
