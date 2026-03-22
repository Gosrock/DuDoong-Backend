package band.gosrock.domain.domains.issuedTicket.domain

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class IssuedTicketItemInfoVoTest {

    var itemInfoVo: IssuedTicketItemInfoVo? = null

    private val ticketType: TicketType = TicketType.FIRST_COME_FIRST_SERVED

    private val payType: TicketPayType = TicketPayType.DUDOONG_TICKET

    private val w3000: Money = Money.wons(3000L)

    private val startAt: LocalDateTime = LocalDateTime.now()

    private val endAt: LocalDateTime = startAt.plusDays(1L)

    @BeforeEach
    fun setUp() {
        itemInfoVo = IssuedTicketItemInfoVo(1L, ticketType, payType, "testTicket", w3000)
    }

    @Test
    fun 티켓_아이템_정보를_발급티켓_아이템_인포로_정상적으로_변환_테스트() {
        // given
        val newTicketItem = TicketItem(
            TicketPayType.DUDOONG_TICKET, "testTicket", "test", w3000, 1L, 1L, 1L, ticketType,
            "test", "test", "test", true, true, startAt, endAt, 1L
        )

        // when
        val itemInfoVoForTest = IssuedTicketItemInfoVo.from(newTicketItem)

        // then
        assertAll(
            { assertEquals(itemInfoVo!!.price, itemInfoVoForTest.price) },
            { assertEquals(itemInfoVo!!.ticketName, itemInfoVoForTest.ticketName) },
            { assertEquals(itemInfoVo!!.ticketType, itemInfoVoForTest.ticketType) },
            { assertEquals(itemInfoVo!!.payType, itemInfoVoForTest.payType) }
        )
    }
}
