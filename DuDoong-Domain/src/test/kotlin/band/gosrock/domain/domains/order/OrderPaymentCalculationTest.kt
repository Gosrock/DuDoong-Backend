package band.gosrock.domain.domains.order

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderCouponVo
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OrderPaymentCalculationTest {

    @Mock
    private lateinit var lineItem1: OrderLineItem

    @Mock
    private lateinit var lineItem2: OrderLineItem

    @Mock
    private lateinit var orderCouponVo: OrderCouponVo

    private lateinit var noCouponOrder: Order
    private lateinit var couponOrder: Order

    @BeforeEach
    fun setUp() {
        noCouponOrder = Order.builder()
            .userId(1L)
            .orderName("쿠폰없는주문")
            .orderLineItems(listOf(lineItem1, lineItem2))
            .orderStatus(OrderStatus.PENDING_PAYMENT)
            .orderMethod(OrderMethod.PAYMENT)
            .eventId(100L)
            .build()

        couponOrder = Order.builder()
            .userId(1L)
            .orderName("쿠폰있는주문")
            .orderLineItems(listOf(lineItem1, lineItem2))
            .orderStatus(OrderStatus.PENDING_PAYMENT)
            .orderMethod(OrderMethod.PAYMENT)
            .eventId(100L)
            .build()
        couponOrder.attachCoupon(orderCouponVo)
    }

    // ---- 할인 없는 주문 ----

    @Test
    fun `쿠폰 없는 주문의 총 할인금액은 0원이다`() {
        assertEquals(Money.ZERO, noCouponOrder.getTotalDiscountPrice())
    }

    @Test
    fun `총 공급가액은 라인아이템 합계다`() {
        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.wons(3000L))
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.wons(4000L))

        assertEquals(Money.wons(7000L), noCouponOrder.getTotalSupplyPrice())
    }

    @Test
    fun `쿠폰 없을때 총 결제금액은 총 공급가액과 같다`() {
        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.wons(3000L))
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.wons(4000L))

        assertEquals(Money.wons(7000L), noCouponOrder.getTotalPaymentPrice())
    }

    // ---- 쿠폰 있는 주문 ----

    @Test
    fun `쿠폰 할인 후 결제금액은 공급가액에서 할인액을 뺀 금액이다`() {
        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.wons(3000L))
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.wons(4000L))
        given(orderCouponVo.discountAmount).willReturn(Money.wons(2000L))

        assertEquals(Money.wons(5000L), couponOrder.getTotalPaymentPrice())
    }

    @Test
    fun `쿠폰으로 전액 할인되면 결제금액이 0원이다`() {
        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.wons(3000L))
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.wons(4000L))
        given(orderCouponVo.discountAmount).willReturn(Money.wons(7000L))

        assertEquals(Money.ZERO, couponOrder.getTotalPaymentPrice())
    }

    // ---- isNeedPaid ----

    @Test
    fun `결제금액이 0원보다 크고 PAYMENT 방식이면 isNeedPaid가 true다`() {
        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.wons(1000L))
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.wons(1000L))

        assertTrue(noCouponOrder.isNeedPaid())
    }

    @Test
    fun `쿠폰으로 전액 할인되면 isNeedPaid가 false다`() {
        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.wons(3000L))
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.wons(4000L))
        given(orderCouponVo.discountAmount).willReturn(Money.wons(7000L))

        assertFalse(couponOrder.isNeedPaid())
    }

    // ---- hasCoupon ----

    @Test
    fun `쿠폰 없는 주문의 hasCoupon은 false다`() {
        assertFalse(noCouponOrder.hasCoupon())
    }

    @Test
    fun `쿠폰 있는 주문의 hasCoupon은 true다`() {
        given(orderCouponVo.isDefault()).willReturn(false)
        assertTrue(couponOrder.hasCoupon())
    }

    // ---- getTotalQuantity ----

    @Test
    fun `getTotalQuantity는 모든 라인아이템의 수량 합계를 반환한다`() {
        given(lineItem1.quantity).willReturn(2L)
        given(lineItem2.quantity).willReturn(3L)

        assertEquals(5L, noCouponOrder.getTotalQuantity())
    }

    // ---- fail ----

    @Test
    fun `fail 호출 시 orderStatus가 FAILED로 변경된다`() {
        noCouponOrder.fail()
        assertEquals(OrderStatus.FAILED, noCouponOrder.orderStatus)
    }

    // ---- isDudoongTicketOrder ----

    @Test
    fun `결제금액이 0원이고 APPROVAL 방식이면 isDudoongTicketOrder가 false다`() {
        val approvalOrder = Order.builder()
            .userId(1L)
            .orderName("승인주문")
            .orderLineItems(listOf(lineItem1, lineItem2))
            .orderStatus(OrderStatus.PENDING_APPROVE)
            .orderMethod(OrderMethod.APPROVAL)
            .eventId(100L)
            .build()

        given(lineItem1.getTotalOrderLinePrice()).willReturn(Money.ZERO)
        given(lineItem2.getTotalOrderLinePrice()).willReturn(Money.ZERO)

        assertFalse(approvalOrder.isDudoongTicketOrder())
    }
}
