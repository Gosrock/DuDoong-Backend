package band.gosrock.api.v2.operation.usecase

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** R-6 '결제 방식' 열 (#740): v1 주문(결제 방식 기록 없음)까지 단위로 고정 */
@DisplayName("v2 주문 엑셀 결제 방식 열")
class V2PaymentChannelLabelTest {

    private fun order(channel: OrderPaymentChannel?, amount: Long, method: OrderMethod?): Order = mock(Order::class.java).also {
        `when`(it.paymentChannel).thenReturn(channel)
        `when`(it.getTotalPaymentPrice()).thenReturn(Money.wons(amount))
        `when`(it.orderMethod).thenReturn(method)
    }

    private fun label(channel: OrderPaymentChannel?, amount: Long, method: OrderMethod?) =
        V2ReadOrdersUseCase.paymentChannelLabel(order(channel, amount, method))

    @Test
    fun `v2 주문은 저장된 결제 방식`() {
        assertEquals("계좌이체", label(OrderPaymentChannel.BANK_TRANSFER, 5000, OrderMethod.APPROVAL))
        assertEquals("토스 송금", label(OrderPaymentChannel.TOSS_TRANSFER, 5000, OrderMethod.APPROVAL))
        assertEquals("무료", label(OrderPaymentChannel.FREE, 0, OrderMethod.PAYMENT))
    }

    @Test
    fun `v1 주문 — 무료는 '무료', 카드(PG) 결제는 'PG 결제', 두둥티켓 승인형은 빈 칸`() {
        assertEquals("무료", label(null, 0, OrderMethod.PAYMENT))
        assertEquals("무료", label(null, 0, OrderMethod.APPROVAL))
        assertEquals("PG 결제", label(null, 5000, OrderMethod.PAYMENT))
        assertEquals("", label(null, 5000, OrderMethod.APPROVAL))
    }
}
