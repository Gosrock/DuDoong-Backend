package band.gosrock.domain.domains.order.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.cart.domain.CartOptionAnswer;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderLineItemTest {

    @Mock OrderOptionAnswer orderOptionAnswer1;

    @Mock OrderOptionAnswer orderOptionAnswer2;
    @Mock CartLineItem cartLineItem;
    Long quantity = 2L;

    @Mock TicketItem ticketItem;
    @Mock OrderItemVo orderItem;

    Money money3000 = Money.wons(3000L);

    // for test
    OrderLineItem orderLineItem;

    @BeforeEach
    void setUp() {

        orderLineItem =
                OrderLineItem.builder()
                        .orderOptionAnswer(List.of(orderOptionAnswer1, orderOptionAnswer2))
                        .quantity(quantity)
                        .orderItemVo(orderItem)
                        .build();
    }

    @Test
    void 아이템_가격_조회_검증() {
        // given
        given(orderItem.getPrice()).willReturn(money3000);
        // when
        Money itemPrice = orderLineItem.getItemPrice();

        // then
        assertEquals(itemPrice, money3000);
    }

    @Test
    void 옵션_총_가격_조회_검증() {
        // given
        Money optionAnswerPrice1 = Money.wons(1000L);
        given(orderOptionAnswer1.getAdditionalPrice()).willReturn(optionAnswerPrice1);
        Money optionAnswerPrice2 = Money.wons(2000L);
        given(orderOptionAnswer2.getAdditionalPrice()).willReturn(optionAnswerPrice2);

        // when
        Money totalOptionAnswersPrice = orderLineItem.getOptionAnswersPrice();
        // then
        assertEquals(totalOptionAnswersPrice, optionAnswerPrice1.plus(optionAnswerPrice2));
    }

    @Test
    void 오더라인_총_가격_조회_검증() {
        // given
        Money optionAnswerPrice1 = Money.wons(1000L);
        given(orderOptionAnswer1.getAdditionalPrice()).willReturn(optionAnswerPrice1);
        Money optionAnswerPrice2 = Money.wons(2000L);
        given(orderOptionAnswer2.getAdditionalPrice()).willReturn(optionAnswerPrice2);
        given(orderItem.getPrice()).willReturn(money3000);
        // when
        Money totalOrderLinePrice = orderLineItem.getTotalOrderLinePrice();
        // then
        Money total = optionAnswerPrice1.plus(optionAnswerPrice2).plus(money3000).times(quantity);
        assertEquals(totalOrderLinePrice, total);
    }

    @Test
    void 옵션에_가격이_붙으면_결제가_필요한_오더라인이다() {
        // given
        given(orderItem.getPrice()).willReturn(money3000);
        Money optionAnswerPrice1 = Money.wons(1000L);
        given(orderOptionAnswer1.getAdditionalPrice()).willReturn(optionAnswerPrice1);
        Money optionAnswerPrice2 = Money.wons(2000L);
        given(orderOptionAnswer2.getAdditionalPrice()).willReturn(optionAnswerPrice2);
        // when
        Boolean needPayment = orderLineItem.isNeedPaid();

        assertTrue(needPayment);
    }

    @Test
    void 아이템에_가격이_있으면_결제가_필요한_오더라인이다() {
        // given
        given(orderItem.getPrice()).willReturn(money3000);
        given(orderOptionAnswer1.getAdditionalPrice()).willReturn(Money.ZERO);
        given(orderOptionAnswer2.getAdditionalPrice()).willReturn(Money.ZERO);
        // when
        Boolean needPayment = orderLineItem.isNeedPaid();

        assertTrue(needPayment);
    }

    @Test
    void 가격이없는_오더라인이면_결제가_필요하지않다() {
        // given
        given(orderItem.getPrice()).willReturn(Money.ZERO);

        orderLineItem =
                OrderLineItem.builder()
                        .orderOptionAnswer(List.of(orderOptionAnswer1, orderOptionAnswer2))
                        .quantity(quantity)
                        .orderItemVo(orderItem)
                        .build();

        given(orderOptionAnswer1.getAdditionalPrice()).willReturn(Money.ZERO);
        given(orderOptionAnswer2.getAdditionalPrice()).willReturn(Money.ZERO);
        // when
        Boolean needPayment = orderLineItem.isNeedPaid();

        assertFalse(needPayment);
    }

    @Test
    public void 주문라인_아이템아이디_조회_검증() {
        // given
        long itemId = 1L;
        given(orderItem.getItemId()).willReturn(itemId);
        // when
        Long findItemId = orderLineItem.getItemId();
        // then
        assertEquals(findItemId, itemId);
    }

    @Test
    public void 주문라인_아이템그룹아이디_조회_검증() {
        // given
        long itemGroupId = 1L;
        given(orderItem.getItemGroupId()).willReturn(itemGroupId);
        // when
        Long findItemGroupId = orderLineItem.getItemGroupId();
        // then
        assertEquals(findItemGroupId, itemGroupId);
    }

    @Test
    public void 주문라인_아이템이름_조회_검증() {
        // given
        String name = "아이템이름";
        given(orderItem.getName()).willReturn(name);
        // when
        String itemName = orderLineItem.getItemName();
        // then
        assertEquals(itemName, name);
    }

    @Test
    public void 주문라인_정적팩터리_메서드_검증() {
        // given
        List<CartOptionAnswer> emptyCartOptionAnswer = List.of();
        List<OrderOptionAnswer> emptyOrderOptionAnswers =
                emptyCartOptionAnswer.stream().map(OrderOptionAnswer::from).toList();
        given(cartLineItem.getCartOptionAnswers()).willReturn(emptyCartOptionAnswer);
        given(cartLineItem.getQuantity()).willReturn(quantity);
        // when
        OrderLineItem build = OrderLineItem.of(cartLineItem, ticketItem);
        // then
        assertEquals(build.getOrderOptionAnswers(), emptyOrderOptionAnswers);
        assertEquals(build.getQuantity(), quantity);
    }

    @Test
    public void 주문라인_옵션아이디조회_검증() {
        // given
        long optionId1 = 1L;
        given(orderOptionAnswer1.getOptionId()).willReturn(optionId1);
        long optionId2 = 2L;
        given(orderOptionAnswer2.getOptionId()).willReturn(optionId2);
        List<Long> optionIds = List.of(optionId1, optionId2);
        // when
        List<Long> answerOptionIds = orderLineItem.getAnswerOptionIds();
        // then
        assertEquals(answerOptionIds, optionIds);
    }
}
