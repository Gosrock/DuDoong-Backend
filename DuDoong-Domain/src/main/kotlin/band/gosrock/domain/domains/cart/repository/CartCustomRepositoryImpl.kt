package band.gosrock.domain.domains.cart.repository

import band.gosrock.domain.domains.cart.domain.Cart
import band.gosrock.domain.domains.cart.domain.QCart.cart
import band.gosrock.domain.domains.cart.domain.QCartLineItem.cartLineItem
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional

class CartCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : CartCustomRepository {

    override fun find(cartId: Long): Optional<Cart> {
        val findCart = queryFactory
            .selectFrom(cart)
            .leftJoin(cart.cartLineItems, cartLineItem)
            .fetchJoin()
            .where(cart.id.eq(cartId))
            .fetchOne()
        return Optional.ofNullable(findCart)
    }
}
