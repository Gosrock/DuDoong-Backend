package band.gosrock.domain.domains.cart.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import band.gosrock.domain.domains.cart.exception.CartItemNotOneTypeException;
import band.gosrock.domain.domains.cart.exception.CartNotAnswerAllOptionGroupException;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.EventNotOpenException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.NotCorrectOptionAnswerException;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
import band.gosrock.domain.domains.ticket_item.exception.TicketPurchaseLimitException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartValidatorTest {

    @Mock TicketItemAdaptor ticketItemAdaptor;

    @Mock OptionAdaptor optionAdaptor;

    @Mock Cart cart;
    @Mock CartLineItem cartLineItem;
    @Mock CartOptionAnswer cartOptionAnswer;
    @Mock Option optionOfGroup1;
    @Mock Option optionOfGroup2;
    @Mock Event event;
    @Mock TicketItem item;

    @Mock IssuedTicketAdaptor issuedTicketAdaptor;
    @Mock EventAdaptor eventAdaptor;

    CartValidator cartValidator;

    @BeforeEach
    void setUp() {
        cartValidator =
                new CartValidator(
                        ticketItemAdaptor, issuedTicketAdaptor, eventAdaptor, optionAdaptor);
    }

    @Test
    public void 카트_티켓팅_가능시간검증_실패() {
        // given
        given(event.isTimeBeforeStartAt()).willReturn(Boolean.FALSE);
        willCallRealMethod().given(event).validateTicketingTime();
        // when
        // then
        assertThrows(
                EventTicketingTimeIsPassedException.class,
                () -> cartValidator.validTicketingTime(event));
    }

    @Test
    public void 카트_티켓팅_가능시간검증_성공() {
        // given
        given(event.isTimeBeforeStartAt()).willReturn(Boolean.TRUE);
        willCallRealMethod().given(event).validateTicketingTime();
        // when
        cartValidator.validTicketingTime(event);
        // then
    }

    @Test
    public void 카트_티켓팅_재고검증_실패() {
        // given
        willThrow(TicketItemQuantityLackException.class).given(item).validEnoughQuantity(any());
        // when
        // then
        assertThrows(
                TicketItemQuantityLackException.class,
                () -> cartValidator.validItemStockEnough(cart, item));
    }

    @Test
    public void 카트_티켓팅_재고검증_성공() {
        // given
        willDoNothing().given(item).validEnoughQuantity(any());
        // when
        cartValidator.validItemStockEnough(cart, item);
        // then
    }

    @Test
    public void 카트_티켓팅_이벤트_상태검증_성공() {
        // given
        willDoNothing().given(event).validateStatusOpen();
        // when
        cartValidator.validEventIsOpen(event);
        // then
    }

    @Test
    public void 카트_티켓팅_이벤트_상태검증_실패() {
        // given
        willThrow(EventNotOpenException.class).given(event).validateStatusOpen();
        // when
        // then
        assertThrows(EventNotOpenException.class, () -> cartValidator.validEventIsOpen(event));
    }

    @Test
    public void 카트_설문지_전부응답검증_성공() {
        // given
        given(cart.getCartLineItems()).willReturn(List.of(cartLineItem));
        given(optionAdaptor.findAllByIds(any()))
                .willReturn(List.of(optionOfGroup1, optionOfGroup2));
        Long optionGroup1Id = 1L;
        Long optionGroup2Id = 2L;

        given(optionOfGroup1.getOptionGroupId()).willReturn(optionGroup1Id);
        given(optionOfGroup2.getOptionGroupId()).willReturn(optionGroup2Id);
        given(item.getOptionGroupIds()).willReturn(List.of(optionGroup1Id, optionGroup2Id));
        // when
        cartValidator.validAnswerToAllQuestion(cart, item);
        // then
    }

    @Test
    public void 카트_설문지_전부응답검증_적게대답_실패() {
        // given
        given(cart.getCartLineItems()).willReturn(List.of(cartLineItem));
        given(optionAdaptor.findAllByIds(any()))
                .willReturn(List.of(optionOfGroup1, optionOfGroup2));
        Long optionGroup1Id = 1L;
        Long optionGroup2Id = 2L;

        given(optionOfGroup1.getOptionGroupId()).willReturn(optionGroup1Id);
        given(optionOfGroup2.getOptionGroupId()).willReturn(optionGroup2Id);
        Long optionGroup3Id = 3L;
        given(item.getOptionGroupIds())
                .willReturn(List.of(optionGroup1Id, optionGroup2Id, optionGroup3Id));
        // when
        // then
        assertThrows(
                CartNotAnswerAllOptionGroupException.class,
                () -> cartValidator.validAnswerToAllQuestion(cart, item));
    }

    @Test
    public void 카트_설문지_전부응답검증_많게대답_실패() {
        // given
        given(cart.getCartLineItems()).willReturn(List.of(cartLineItem));
        given(optionAdaptor.findAllByIds(any()))
                .willReturn(List.of(optionOfGroup1, optionOfGroup2));
        Long optionGroup1Id = 1L;
        Long optionGroup2Id = 2L;

        given(optionOfGroup1.getOptionGroupId()).willReturn(optionGroup1Id);
        given(optionOfGroup2.getOptionGroupId()).willReturn(optionGroup2Id);
        given(item.getOptionGroupIds()).willReturn(List.of(optionGroup1Id));
        // when
        // then
        assertThrows(
                CartNotAnswerAllOptionGroupException.class,
                () -> cartValidator.validAnswerToAllQuestion(cart, item));
    }

    @Test
    public void 카트_설문지_전부응답검증_이상하게대답_실패() {
        // given
        given(cart.getCartLineItems()).willReturn(List.of(cartLineItem));
        given(optionAdaptor.findAllByIds(any()))
                .willReturn(List.of(optionOfGroup1, optionOfGroup2));
        Long optionGroup1Id = 1L;
        Long optionGroup2Id = 2L;

        given(optionOfGroup1.getOptionGroupId()).willReturn(optionGroup1Id);
        given(optionOfGroup2.getOptionGroupId()).willReturn(optionGroup2Id);
        Long optionGroup3Id = 3L;
        given(item.getOptionGroupIds()).willReturn(List.of(optionGroup1Id, optionGroup3Id));
        // when
        // then
        assertThrows(
                CartNotAnswerAllOptionGroupException.class,
                () -> cartValidator.validAnswerToAllQuestion(cart, item));
    }

    @Test
    public void 카트_설문지_올바른응답검증_이상하게대답_실패() {
        // given
        given(cart.getCartLineItems()).willReturn(List.of(cartLineItem));
        given(cartLineItem.getCartOptionAnswers()).willReturn(List.of(cartOptionAnswer));
        Long optionId = 1L;
        given(cartOptionAnswer.getOptionId()).willReturn(optionId);
        given(cartOptionAnswer.getAnswer()).willReturn("이상하게대답");

        given(optionAdaptor.findAllByIds(any())).willReturn(List.of(optionOfGroup1));

        given(optionOfGroup1.getId()).willReturn(optionId);
        willThrow(NotCorrectOptionAnswerException.class)
                .given(optionOfGroup1)
                .validCorrectAnswer("이상하게대답");
        // when
        // then
        assertThrows(
                NotCorrectOptionAnswerException.class,
                () -> cartValidator.validCorrectAnswer(cart));
    }

    @Test
    public void 카트_설문지_올바른응답검증_올바르게대답_성공() {
        // given
        given(cart.getCartLineItems()).willReturn(List.of(cartLineItem));
        given(cartLineItem.getCartOptionAnswers()).willReturn(List.of(cartOptionAnswer));
        Long optionId = 1L;
        given(cartOptionAnswer.getOptionId()).willReturn(optionId);
        given(cartOptionAnswer.getAnswer()).willReturn("예");

        given(optionAdaptor.findAllByIds(any())).willReturn(List.of(optionOfGroup1));

        given(optionOfGroup1.getId()).willReturn(optionId);
        willDoNothing().given(optionOfGroup1).validCorrectAnswer("예");

        // when
        cartValidator.validCorrectAnswer(cart);
        // then
    }

    @Test
    public void 카트_아이템_한종류가아니면_실패() {
        // given
        given(cart.getDistinctItemIds()).willReturn(List.of(1L, 2L));
        // then
        assertThrows(
                CartItemNotOneTypeException.class,
                () -> cartValidator.validItemKindIsOneType(cart));
    }

    @Test
    public void 카트_아이템_구매갯수제한_실패() {
        // given
        given(cart.getTotalQuantity()).willReturn(3L);
        willThrow(TicketPurchaseLimitException.EXCEPTION).given(item).validPurchaseLimit(any());
        // then
        assertThrows(
                TicketPurchaseLimitException.class,
                () -> cartValidator.validItemPurchaseLimit(cart, item));
    }

    @Test
    public void 카트_아이템_한종류면_성공() {
        // given
        given(cart.getDistinctItemIds()).willReturn(List.of(2L));
        // then
        cartValidator.validItemKindIsOneType(cart);
    }
}
