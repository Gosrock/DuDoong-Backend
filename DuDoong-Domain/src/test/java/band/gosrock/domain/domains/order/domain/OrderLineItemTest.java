package band.gosrock.domain.domains.order.domain;

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
class OrderLineItemTest {

    @Mock OrderOptionAnswer orderOptionAnswer1;

    @Mock OrderOptionAnswer orderOptionAnswer2;
    Long quantity = 2L;

    @Mock TicketItem ticketItem;

    Money money3000 = Money.wons(3000L);

    // for test
    OrderLineItem orderLineItem;

    @BeforeEach
    void setUp() {
        orderLineItem =
                OrderLineItem.builder()
                        .orderOptionAnswer(List.of(orderOptionAnswer1, orderOptionAnswer2))
                        .quantity(quantity)
                        .ticketItem(ticketItem)
                        .build();
    }

    @Test
    void 아이템_가격_조회_검증() {
        // given
        given(ticketItem.getPrice()).willReturn(money3000);

        // when
        Money itemPrice = orderLineItem.getItemPrice();

        // then
        assertEquals(itemPrice, money3000);
    }

    @Test
    void 옵션_총_가격_조회_검증() {
        // given
        Money optionAnswerPrice1 = Money.wons(1000L);
        given(orderOptionAnswer1.getOptionPrice()).willReturn(optionAnswerPrice1);
        Money optionAnswerPrice2 = Money.wons(2000L);
        given(orderOptionAnswer2.getOptionPrice()).willReturn(optionAnswerPrice2);

        // when
        Money totalOptionAnswersPrice = orderLineItem.getOptionAnswersPrice();
        // then
        assertEquals(totalOptionAnswersPrice, optionAnswerPrice1.plus(optionAnswerPrice2));
    }

    @Test
    void 오더라인_총_가격_조회_검증() {
        // given
        Money optionAnswerPrice1 = Money.wons(1000L);
        given(orderOptionAnswer1.getOptionPrice()).willReturn(optionAnswerPrice1);
        Money optionAnswerPrice2 = Money.wons(2000L);
        given(orderOptionAnswer2.getOptionPrice()).willReturn(optionAnswerPrice2);

        given(ticketItem.getPrice()).willReturn(money3000);

        // when
        Money totalOrderLinePrice = orderLineItem.getTotalOrderLinePrice();
        // then
        Money total = optionAnswerPrice1.plus(optionAnswerPrice2).plus(money3000).times(quantity);
        assertEquals(totalOrderLinePrice, total);
    }
}
