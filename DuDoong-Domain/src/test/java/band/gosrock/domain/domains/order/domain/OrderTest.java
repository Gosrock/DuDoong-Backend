package band.gosrock.domain.domains.order.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.exception.NotOwnerOrderException;
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTest {

    @Mock IssuedCoupon issuedCoupon;

    @Mock OrderLineItem orderLineItem1;

    @Mock OrderLineItem orderLineItem2;

    // 조회용테스트 order
    Order notHaveCouponOrder;

    @BeforeEach
    void setUp() {
        notHaveCouponOrder =
                Order.builder()
                        .userId(1L)
                        .orderName("주문이름")
                        .orderLineItems(List.of(orderLineItem1, orderLineItem2))
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderMethod(OrderMethod.APPROVAL)
                        .build();
    }

    @Test
    void 오더라인중에_환불불가한_오더라인이_있다면_주문은_환불불가해야한다() {
        // given
        given(orderLineItem1.canRefund()).willReturn(Boolean.FALSE);
        given(orderLineItem2.canRefund()).willReturn(Boolean.TRUE);

        // when
        Boolean canRefundDate = notHaveCouponOrder.canRefundDate();

        // then
        assertFalse(canRefundDate);
        assertThrows(
                NotRefundAvailableDateOrderException.class,
                () -> notHaveCouponOrder.validCanRefundDate());
    }

    @Test
    void 오더라인중에_환불불가한_오더라인이_없다면_주문은_환불가능해야한다() {
        // given
        given(orderLineItem1.canRefund()).willReturn(Boolean.TRUE);
        given(orderLineItem2.canRefund()).willReturn(Boolean.TRUE);

        // when
        Boolean canRefundDate = notHaveCouponOrder.canRefundDate();

        // then
        assertTrue(canRefundDate);
        notHaveCouponOrder.validCanRefundDate();
    }

    @Test
    void 할인쿠폰이없다면_총할인금액은_0원이다() {
        // given
        // when
        Money totalDiscountPrice = notHaveCouponOrder.getTotalDiscountPrice();
        // then
        assertEquals(totalDiscountPrice, Money.ZERO);
    }

    @Test
    void 총공급가액_계산_검증() {
        // given
        Money wons2000 = Money.wons(3000L);
        Money wons4000 = Money.wons(4000L);
        given(orderLineItem1.getTotalOrderLinePrice()).willReturn(wons2000);
        given(orderLineItem2.getTotalOrderLinePrice()).willReturn(wons4000);
        // when
        Money totalSupplyPrice = notHaveCouponOrder.getTotalSupplyPrice();
        // then
        assertEquals(totalSupplyPrice, wons2000.plus(wons4000));
    }

    @Test
    void 쿠폰_없을때_총결제금액_계산_검증() {
        // given
        Money wons2000 = Money.wons(3000L);
        Money wons4000 = Money.wons(4000L);
        given(orderLineItem1.getTotalOrderLinePrice()).willReturn(wons2000);
        given(orderLineItem2.getTotalOrderLinePrice()).willReturn(wons4000);
        // when
        Money totalPaymentPrice = notHaveCouponOrder.getTotalPaymentPrice();
        // then
        assertEquals(totalPaymentPrice, wons2000.plus(wons4000));
    }

    @Test
    void 주문에_대한_주인인지_검증() {
        // given
        Order user1Order = Order.builder().orderLineItems(List.of()).userId(1L).build();

        // then
        user1Order.validOwner(1L);
        assertThrows(NotOwnerOrderException.class, () -> user1Order.validOwner(2L));
    }
}
