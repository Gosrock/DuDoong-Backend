package band.gosrock.domain.domains.cart.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.exception.CartLineItemNotFoundException
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany

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

        /** 테스트 전용 팩토리 메서드 */
        @JvmStatic
        fun forTest(
            userId: Long? = null,
            cartLineItems: List<CartLineItem> = emptyList(),
        ): Cart = Cart().apply {
            this.userId = userId
            this.cartLineItems.addAll(cartLineItems)
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
