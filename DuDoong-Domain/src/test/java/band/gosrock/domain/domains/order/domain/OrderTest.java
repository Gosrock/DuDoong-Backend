package band.gosrock.domain.domains.order.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTest {

    @Mock OrderCouponVo orderCouponVo;

    @Mock OrderLineItem orderLineItem1;

    @Mock OrderLineItem orderLineItem2;

    // 조회용테스트 order
    Order notHaveCouponOrder;

    Order couponOrder;

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
        couponOrder =
                Order.builder()
                        .userId(1L)
                        .orderName("주문이름")
                        .orderLineItems(List.of(orderLineItem1, orderLineItem2))
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderMethod(OrderMethod.APPROVAL)
                        .build();
        couponOrder.attachCoupon(orderCouponVo);
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
    void 쿠폰_있을때_총결제금액_계산_검증() {
        // given
        Money wons3000 = Money.wons(3000L);
        Money wons4000 = Money.wons(4000L);
        given(orderLineItem1.getTotalOrderLinePrice()).willReturn(wons3000);
        given(orderLineItem2.getTotalOrderLinePrice()).willReturn(wons4000);
        given(orderCouponVo.getDiscountAmount()).willReturn(wons4000);

        // when
        Money totalPaymentPrice = couponOrder.getTotalPaymentPrice();
        // then
        assertEquals(totalPaymentPrice, wons3000.plus(wons4000).minus(wons4000));
    }

    @Test
    void 쿠폰_으로_0원_결제가능() {
        // given
        Money wons3000 = Money.wons(3000L);
        Money wons4000 = Money.wons(4000L);
        given(orderLineItem1.getTotalOrderLinePrice()).willReturn(wons3000);
        given(orderLineItem2.getTotalOrderLinePrice()).willReturn(wons4000);
        given(orderCouponVo.getDiscountAmount()).willReturn(wons4000.plus(wons3000));

        // when
        Money totalPaymentPrice = couponOrder.getTotalPaymentPrice();
        // then
        assertEquals(totalPaymentPrice, Money.ZERO);
        assertFalse(couponOrder.isNeedPaid());
    }

    @Test
    void 주문번호생성_검증() {
        // given
        Money wons3000 = Money.wons(3000L);
        Money wons4000 = Money.wons(4000L);
        given(orderLineItem1.getTotalOrderLinePrice()).willReturn(wons3000);
        given(orderLineItem2.getTotalOrderLinePrice()).willReturn(wons4000);
        given(orderCouponVo.getDiscountAmount()).willReturn(wons4000.plus(wons3000));

        // when
        Money totalPaymentPrice = couponOrder.getTotalPaymentPrice();
        // then
        assertEquals(totalPaymentPrice, Money.ZERO);
        assertFalse(couponOrder.isNeedPaid());
    }
}
