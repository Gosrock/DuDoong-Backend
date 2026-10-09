package band.gosrock.admin.service

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** 운영 재고 조정: 경로의 공연과 티켓 소속이 다르면 거부 (#762 L-7) */
class AdminAdjustTicketStockUseCaseTest {

    private val ticketItemAdaptor = mock(TicketItemAdaptor::class.java)
    private val useCase = AdminAdjustTicketStockUseCase(ticketItemAdaptor, mock(AdminAuthValidator::class.java))
    private val item = TicketItem(
        payType = TicketPayType.FREE_TICKET,
        name = "무료",
        price = Money.ZERO,
        quantity = 10,
        supplyCount = 10,
        purchaseLimit = 4,
        type = TicketType.FIRST_COME_FIRST_SERVED,
        isSellable = true,
        eventId = 10L,
    ).also { ReflectionTestUtils.setField(it, "id", 1L) }

    @Test
    fun `다른 공연의 티켓이면 거부하고 재고를 바꾸지 않는다`() {
        `when`(ticketItemAdaptor.queryTicketItem(1L)).thenReturn(item)

        assertThrows<InvalidTicketItemException> { useCase.execute(99L, 20L, 1L, 5) }
        assertEquals(10L, item.quantity)
        verify(ticketItemAdaptor, never()).save(item)
    }

    @Test
    fun `같은 공연의 티켓이면 조정한다`() {
        `when`(ticketItemAdaptor.queryTicketItem(1L)).thenReturn(item)

        useCase.execute(99L, 10L, 1L, 5)

        assertEquals(15L, item.quantity)
        verify(ticketItemAdaptor).save(item)
    }
}
