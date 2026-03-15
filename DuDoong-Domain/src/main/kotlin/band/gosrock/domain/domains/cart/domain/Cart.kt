package band.gosrock.domain.domains.cart.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.exception.CartLineItemNotFoundException
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

@Entity(name = "tbl_cart")
class Cart() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    var id: Long? = null
        protected set

    @Column(nullable = false)
    var userId: Long? = null
        protected set

    var cartName: String? = null
        protected set

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    var cartLineItems: MutableList<CartLineItem> = mutableListOf()
        protected set

    companion object {
        @JvmStatic
        fun of(
            cartLineItems: List<CartLineItem>,
            itemName: String,
            userId: Long,
            cartValidator: CartValidator,
        ): Cart = Cart().apply {
            this.userId = userId
            this.cartLineItems.addAll(cartLineItems)
            cartValidator.validCanCreate(this)
            updateCartName(itemName)
        }

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        private var cartLineItems: List<CartLineItem> = emptyList()
        fun userId(userId: Long) = apply { this.userId = userId }
        fun cartLineItems(cartLineItems: List<CartLineItem>) = apply { this.cartLineItems = cartLineItems }
        fun build(): Cart = Cart().apply {
            this.userId = this@Builder.userId
            this.cartLineItems.addAll(this@Builder.cartLineItems)
        }
    }

    fun updateCartName(name: String) {
        cartName = name
    }

    fun isNeedPaid(): Boolean =
        cartLineItems.any { it.isNeedPaid() }

    fun getTotalQuantity(): Long =
        cartLineItems.sumOf { it.quantity!! }

    fun getTotalPrice(): Money =
        cartLineItems.fold(Money.ZERO) { acc, item -> acc.plus(item.getTotalCartLinePrice()) }

    fun getItemId(): Long = getCartLineItem().itemId!!

    fun getCartLineItem(): CartLineItem =
        cartLineItems.firstOrNull() ?: throw CartLineItemNotFoundException.EXCEPTION

    fun getDistinctItemIds(): List<Long> =
        cartLineItems.map { it.itemId!! }.distinct()
}
