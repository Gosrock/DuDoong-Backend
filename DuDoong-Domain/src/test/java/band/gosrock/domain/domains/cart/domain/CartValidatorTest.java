package band.gosrock.domain.domains.cart.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.EventIsNotOpenStatusException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
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

    @Mock Event event;

    @Mock TicketItem item;

    CartValidator cartValidator;

    @BeforeEach
    void setUp() {
        cartValidator = new CartValidator(ticketItemAdaptor, optionAdaptor);
    }

    @Test
    public void 카트_티켓팅_가능시간검증_실패() {
        // given
        given(event.isTimeBeforeStartAt()).willReturn(Boolean.FALSE);
        willCallRealMethod().given(event).validTicketingTime();
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
        willCallRealMethod().given(event).validTicketingTime();
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
        willDoNothing().given(event).validStatusOpen();
        // when
        cartValidator.validEventIsOpen(event);
        // then
    }

    @Test
    public void 카트_티켓팅_이벤트_상태검증_실패() {
        // given
        willThrow(EventIsNotOpenStatusException.class).given(event).validStatusOpen();
        // when
        // then
        assertThrows(
                EventIsNotOpenStatusException.class, () -> cartValidator.validEventIsOpen(event));
    }

    @Test
    public void 카트_설문지_전부응답검증_성공() {
        // given
        willThrow(EventIsNotOpenStatusException.class).given(event).validStatusOpen();
        // when
        // then
        assertThrows(
                EventIsNotOpenStatusException.class, () -> cartValidator.validEventIsOpen(event));
    }
}
