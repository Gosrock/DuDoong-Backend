package band.gosrock.api.v2.operation.usecase

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketQuery
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** 발급 티켓 주문자·소유자 이름 대체 규칙 (#740 리뷰): 회원 행이 없을 때 */
@DisplayName("v2 발급 티켓 주문자·소유자 이름")
class V2TicketPeopleTest {

    private val userAdaptor = mock(UserAdaptor::class.java)
    private val orderAdaptor = mock(OrderAdaptor::class.java)
    private val query = mock(V2IssuedTicketQuery::class.java)
    private val mapper = V2OperationMapper(
        userAdaptor, mock(EventAdaptor::class.java), mock(HostAdaptor::class.java), orderAdaptor, mock(OptionAdaptor::class.java), query,
    )

    private fun user(id: Long, name: String) = User(profile = Profile(name = name)).also { ReflectionTestUtils.setField(it, "id", id) }

    private fun ticket(ownerId: Long, snapshotName: String, orderUuid: String?) = IssuedTicket(
        eventId = 1L,
        userInfo = IssuedTicketUserInfoVo(userId = ownerId, userName = snapshotName),
        orderUuid = orderUuid,
    ).also { ReflectionTestUtils.setField(it, "id", ownerId * 100) }

    private fun order(uuid: String, userId: Long) = Order().also {
        ReflectionTestUtils.setField(it, "uuid", uuid)
        ReflectionTestUtils.setField(it, "userId", userId)
    }

    private fun names(ticket: IssuedTicket, orders: List<Order>, users: List<User>): Pair<String?, String?> {
        `when`(orderAdaptor.findByUuidIn(anyList())).thenReturn(orders)
        `when`(userAdaptor.findUserByIdIn(anyList())).thenReturn(users)
        `when`(query.giftStatesOf(anyCollection())).thenReturn(emptyMap())
        return mapper.ticketViewsOf(listOf(ticket)).single().element.let { it.buyerName to it.ownerName }
    }

    @Test
    fun `회원 행이 다 있으면 주문자·소유자 현재 이름`() {
        assertEquals("주문자" to "소유자", names(ticket(2, "발급시점", "o1"), listOf(order("o1", 1)), listOf(user(1, "주문자"), user(2, "소유자"))))
    }

    @Test
    fun `주문자 회원 행이 없고 주문자 = 소유자면 소유자 이름으로 채운다`() {
        assertEquals("소유자" to "소유자", names(ticket(2, "발급시점", "o1"), listOf(order("o1", 2)), emptyList<User>() + user(2, "소유자")))
        // 소유자 회원 행도 없으면 발급 시점 이름
        assertEquals("발급시점" to "발급시점", names(ticket(2, "발급시점", "o1"), listOf(order("o1", 2)), emptyList()))
    }

    @Test
    fun `주문자 회원 행이 없고 주문자 != 소유자면 주문자 이름은 null (받은 사람 이름을 주문자로 보이지 않게)`() {
        val (buyer, owner) = names(ticket(2, "발급시점", "o1"), listOf(order("o1", 1)), listOf(user(2, "소유자")))
        assertNull(buyer)
        assertEquals("소유자", owner)
    }

    @Test
    fun `주문이 없으면(방어) 소유자 이름`() {
        assertEquals("소유자" to "소유자", names(ticket(2, "발급시점", "none"), emptyList(), listOf(user(2, "소유자"))))
    }
}
