package band.gosrock.domain.domains.cart.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartLineItemTest {

    @Mock CartOptionAnswer cartOptionAnswer;

    @Mock TicketItem hasPriceItem;

    @Mock TicketItem freeItem;
    CartLineItem hasPriceCartLineItem;
    CartLineItem freeCartLineItem;

    public static Long itemQuantity = 3L;

    public static Money itemPrice = Money.wons(3000L);

    @BeforeEach
    public void setup() {
        given(hasPriceItem.getId()).willReturn(1L);
        given(hasPriceItem.getPrice()).willReturn(itemPrice);

        hasPriceCartLineItem =
                CartLineItem.builder()
                        .quantity(itemQuantity)
                        .cartOptionAnswers(List.of(cartOptionAnswer))
                        .item(hasPriceItem)
                        .build();

        given(freeItem.getId()).willReturn(1L);
        given(freeItem.getPrice()).willReturn(Money.ZERO);

        freeCartLineItem =
                CartLineItem.builder()
                        .quantity(itemQuantity)
                        .cartOptionAnswers(List.of(cartOptionAnswer))
                        .item(freeItem)
                        .build();
    }

    @Test
    public void 카트라인_총옵션가격_조회() {
        // given
        Money won2000 = Money.wons(2000L);
        given(cartOptionAnswer.getAdditionalPrice()).willReturn(won2000);
        // when
        Money totalOptionsPrice = hasPriceCartLineItem.getTotalOptionsPrice();
        // then
        assertEquals(totalOptionsPrice, won2000);
    }

    @Test
    public void 카트라인_총가격_조회() {
        // given
        Money won2000 = Money.wons(2000L);
        given(cartOptionAnswer.getAdditionalPrice()).willReturn(won2000);
        // when
        Money totalCartLinePrice = hasPriceCartLineItem.getTotalCartLinePrice();
        // then
        assertEquals(totalCartLinePrice, won2000.plus(itemPrice).times(itemQuantity));
    }

    @Test
    public void 카트라인_결제금액_있을때_결제필요_여부_조회() {
        // given
        Money won2000 = Money.wons(2000L);
        given(cartOptionAnswer.getAdditionalPrice()).willReturn(won2000);
        // when
        Boolean needPaid = hasPriceCartLineItem.isNeedPaid();
        // then
        assertTrue(needPaid);
    }

    @Test
    public void 카트라인_결제금액_없을때_결제필요_여부_조회() {
        // given
        given(cartOptionAnswer.getAdditionalPrice()).willReturn(Money.ZERO);
        // when
        Boolean needPaid = freeCartLineItem.isNeedPaid();
        // then
        assertFalse(needPaid);
    }
}
