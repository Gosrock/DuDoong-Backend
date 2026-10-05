package band.gosrock.domain.domains.cart.repository

import band.gosrock.domain.domains.cart.exception.CartLineItemNotFoundException
import band.gosrock.domain.domains.cart.exception.CartNotFoundException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * v1 주문 생성 락 키(장바구니의 티켓 id)를 락 전에 읽는다 (#724). JPA 가 아니라 JDBC 로 읽는 이유: open-in-view 가 켜진 요청에서
 * 요청 EntityManager 로 읽으면 그 커넥션을 요청 끝까지 쥔 채 락을 기다린다(#743·#746 기준). JDBC 조회는 트랜잭션 밖에서 바로 커넥션을 돌려준다.
 * 장바구니 줄은 만든 뒤 바뀌지 않으므로 락 전에 읽어도 된다
 */
@Repository
class CartLockKeyQuery(private val jdbcTemplate: JdbcTemplate) {

    /** 없는·남의 장바구니는 Cart_404_1, 빈 장바구니는 CartLineItemNotFound (예전 `CartAdaptor.queryCart(cartId, userId).getItemId()` 와 같은 예외) */
    fun itemIdOf(cartId: Long, userId: Long): Long {
        val rows = jdbcTemplate.query(
            "SELECT l.item_id FROM tbl_cart c LEFT JOIN tbl_cart_line_item l ON l.cart_id = c.cart_id WHERE c.cart_id = ? AND c.user_id = ? ORDER BY l.cart_line_id",
            { rs, _ -> rs.getLong(1).takeUnless { rs.wasNull() } },
            cartId,
            userId,
        )
        if (rows.isEmpty()) throw CartNotFoundException.EXCEPTION
        return rows.first() ?: throw CartLineItemNotFoundException.EXCEPTION
    }
}
