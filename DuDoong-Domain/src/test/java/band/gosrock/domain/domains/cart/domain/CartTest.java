package band.gosrock.domain.domains.cart.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

import band.gosrock.domain.common.vo.Money;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartTest {

    @Mock CartLineItem cartLineItem1;
    @Mock CartLineItem cartLineItem2;
    @Mock CartValidator cartValidator;

    Cart hasPriceCart;

    Cart freeCart;

    @BeforeEach
    void setUp() {
        hasPriceCart =
                Cart.builder()
                        .cartLineItems(List.of(cartLineItem1, cartLineItem2))
                        .userId(1L)
                        .build();

        freeCart = Cart.builder().cartLineItems(List.of(cartLineItem2)).userId(1L).build();
    }

    @Test
    public void 카트_결제필요여부_로직검증() {
        // given
        // 유로 카트라인
        given(cartLineItem1.isNeedPaid()).willReturn(Boolean.TRUE);
        // 무료 카트라인
        given(cartLineItem2.isNeedPaid()).willReturn(Boolean.FALSE);
        // when
        Boolean priceNeedPaid = hasPriceCart.isNeedPaid();
        Boolean freeNeedPaid = freeCart.isNeedPaid();
        // then
        assertTrue(priceNeedPaid);
        assertFalse(freeNeedPaid);
    }

    @Test
    public void 카트_총금액_조회_로직검증() {
        // given
        // 유로 카트라인
        given(cartLineItem1.getTotalCartLinePrice()).willReturn(Money.wons(3000L));
        // 무료 카트라인
        given(cartLineItem2.getTotalCartLinePrice()).willReturn(Money.ZERO);
        // when
        Money totalPrice1 = hasPriceCart.getTotalPrice();
        Money totalPrice2 = freeCart.getTotalPrice();
        // then
        assertEquals(totalPrice1, Money.wons(3000L));
        assertEquals(totalPrice2, Money.ZERO);
    }

    @Test
    public void 카트_아이템총_수량조회_로직검증() {
        // given
        // 유로 카트라인
        given(cartLineItem1.getQuantity()).willReturn(3L);
        // 무료 카트라인
        given(cartLineItem2.getQuantity()).willReturn(2L);
        // when
        Long totalQuantity = hasPriceCart.getTotalQuantity();
        // then
        assertEquals(totalQuantity, 5L);
    }

    @Test
    public void 카트_아이템아이디_중복제거_조회_로직검증() {
        // given
        // 유로 카트라인
        given(cartLineItem1.getItemId()).willReturn(1L);
        // 무료 카트라인
        given(cartLineItem2.getItemId()).willReturn(1L);
        // when
        List<Long> distinctItemIds = hasPriceCart.getDistinctItemIds();
        // then
        assertEquals(distinctItemIds, List.of(1L));
    }

    @Test
    public void 카트_정적팩터리를_이용한생성시에_올바르게생성했는지검증() {
        // given
        willDoNothing().given(cartValidator).validCanCreate(any());
        List<CartLineItem> cartLineItems = List.of(cartLineItem1);
        Cart buildCart = Cart.builder().userId(1L).cartLineItems(cartLineItems).build();
        buildCart.updateCartName("장바구니이름");
        // when
        Cart cart = Cart.of(cartLineItems, "장바구니이름", 1L, cartValidator);
        // then
        assertEquals(cart.getCartName(), buildCart.getCartName());
    }

    @Test
    public void 카트_장바구니이름_업데이트_검증() {
        // given
        String cartName = "장바구니이름";
        // when
        freeCart.updateCartName(cartName);
        // then
        assertEquals(freeCart.getCartName(), cartName);
    }
}
