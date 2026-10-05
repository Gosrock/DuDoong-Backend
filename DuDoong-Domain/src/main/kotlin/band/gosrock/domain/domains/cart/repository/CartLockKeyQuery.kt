package band.gosrock.domain.domains.cart.repository

import band.gosrock.domain.domains.cart.exception.CartLineItemNotFoundException
import band.gosrock.domain.domains.cart.exception.CartNotFoundException
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * v1 주문 생성 락을 정하는 장바구니 정보 (#724).
 * @property approval 승인형 주문이 되는 티켓인지 — v1 `OrderFactory.createNormalOrder` 와 같은 규칙: 두둥티켓, 또는 선착순이 아닌 무료 티켓.
 *   티켓의 결제 방식·승인 방식은 만든 뒤 바뀌지 않는 속성이라 락 전에 읽어도 된다
 */
data class CartLockKey(val itemId: Long, val approval: Boolean)

/**
 * v1 주문 생성 락 키를 락 전에 읽는다 (#724). JPA 가 아니라 JDBC 로 읽는 이유: open-in-view 가 켜진 요청에서
 * 요청 EntityManager 로 읽으면 그 커넥션을 요청 끝까지 쥔 채 락을 기다린다(#743·#746 기준). JDBC 조회는 트랜잭션 밖에서 바로 커넥션을 돌려준다.
 * 장바구니 줄은 만든 뒤 바뀌지 않는다. 장바구니에는 티켓 종류가 1개뿐이므로(v1 `CartValidator`, 주문 때 `validItemKindIsOneType`) 첫 줄의 티켓이면 된다
 */
@Repository
class CartLockKeyQuery(private val jdbcTemplate: JdbcTemplate) {

    /** 없는·남의 장바구니는 Cart_404_1, 빈 장바구니는 CartLineItemNotFound (예전 `CartAdaptor.queryCart(cartId, userId).getItemId()` 와 같은 예외) */
    fun lockKeyOf(cartId: Long, userId: Long): CartLockKey {
        val rows = jdbcTemplate.query(
            "SELECT l.item_id, t.pay_type, t.type FROM tbl_cart c " +
                "LEFT JOIN tbl_cart_line_item l ON l.cart_id = c.cart_id LEFT JOIN tbl_ticket_item t ON t.ticket_item_id = l.item_id " +
                "WHERE c.cart_id = ? AND c.user_id = ? ORDER BY l.cart_line_id",
            { rs, _ ->
                val itemId = rs.getLong(1).takeUnless { rs.wasNull() }
                itemId?.let { CartLockKey(it, isApproval(rs.getString(2), rs.getString(3))) }
            },
            cartId,
            userId,
        )
        if (rows.isEmpty()) throw CartNotFoundException.EXCEPTION
        return rows.first() ?: throw CartLineItemNotFoundException.EXCEPTION
    }

    private fun isApproval(payType: String?, type: String?): Boolean =
        payType == TicketPayType.DUDOONG_TICKET.name || (payType == TicketPayType.FREE_TICKET.name && type != TicketType.FIRST_COME_FIRST_SERVED.name)
}
