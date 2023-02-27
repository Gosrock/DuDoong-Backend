package band.gosrock.domain.domains.order.domain.validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.EventNotOpenException;
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.exception.CanNotCancelOrderException;
import band.gosrock.domain.domains.order.exception.CanNotRefundOrderException;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotApprovalOrderException;
import band.gosrock.domain.domains.order.exception.NotFreeOrderException;
import band.gosrock.domain.domains.order.exception.NotOwnerOrderException;
import band.gosrock.domain.domains.order.exception.NotPaymentOrderException;
import band.gosrock.domain.domains.order.exception.NotPendingOrderException;
import band.gosrock.domain.domains.order.exception.NotRefundAvailableDateOrderException;
import band.gosrock.domain.domains.order.exception.OrdeItemNotOneTypeException;
import band.gosrock.domain.domains.order.exception.OrderItemOptionChangedException;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException;
import band.gosrock.domain.domains.ticket_item.exception.TicketPurchaseLimitException;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @Mock Order order;

    @Mock Event event;
    @Mock Event availableRefundEvent;
    @Mock Event unavailableRefundEvent;
    @Mock EventAdaptor eventAdaptor;
    @Mock TicketItemAdaptor ticketItemAdaptor;
    @Mock IssuedTicketAdaptor issuedTicketAdaptor;
    @Mock OptionAdaptor optionAdaptor;
    @Mock UserAdaptor userAdaptor;
    @Mock Option optionOfGroup1;
    @Mock Option optionOfGroup2;
    @Mock OrderLineItem orderLineItem;

    @Mock TicketItem item;

    OrderValidator orderValidator;

    @BeforeEach
    void setUp() {
        orderValidator =
                new OrderValidator(
                        eventAdaptor,
                        ticketItemAdaptor,
                        issuedTicketAdaptor,
                        optionAdaptor,
                        userAdaptor);
    }

    @Test
    public void 주문방식_승인주문_검증_실패() {
        // given
        given(order.getOrderMethod()).willReturn(OrderMethod.PAYMENT);
        // when
        // then
        assertThrows(
                NotApprovalOrderException.class,
                () -> orderValidator.validMethodIsCanApprove(order));
    }

    @Test
    public void 주문방식_승인주문_검증_성공() {
        // given
        given(order.getOrderMethod()).willReturn(OrderMethod.APPROVAL);
        // when
        orderValidator.validMethodIsCanApprove(order);
        // then
    }

    @Test
    public void 주인_검증_실패() {
        // given
        long userId = 1L;
        long otherUserId = 2L;
        given(order.getUserId()).willReturn(otherUserId);
        // when
        // then
        assertThrows(NotOwnerOrderException.class, () -> orderValidator.validOwner(order, userId));
    }

    @Test
    public void 주인_검증_성공() {
        // given
        long userId = 1L;
        given(order.getUserId()).willReturn(userId);
        // when
        orderValidator.validOwner(order, userId);
        // then
    }

    @Test
    public void 주문확인_금액검증_성공() {
        // given
        Money won3000 = Money.wons(3000L);
        given(order.getTotalPaymentPrice()).willReturn(won3000);
        // when
        orderValidator.validAmountIsSameAsRequest(order, won3000);
        // then
    }

    @Test
    public void 주문확인_금액검증_실패() {
        // given
        Money won3000 = Money.wons(3000L);
        Money won2000 = Money.wons(2000L);
        given(order.getTotalPaymentPrice()).willReturn(won3000);
        // when
        // then
        assertThrows(
                InvalidOrderException.class,
                () -> orderValidator.validAmountIsSameAsRequest(order, won2000));
    }

    @Test
    public void 주문방식_결제방식검증_성공() {
        // given
        given(order.getOrderMethod()).willReturn(OrderMethod.PAYMENT);

        // when
        orderValidator.validMethodIsPaymentOrder(order);
        // then
    }

    @Test
    public void 주문방식_결제방식검증_실패() {
        // given
        given(order.getOrderMethod()).willReturn(OrderMethod.APPROVAL);
        // when
        // then
        assertThrows(
                NotPaymentOrderException.class,
                () -> orderValidator.validMethodIsPaymentOrder(order));
    }

    @Test
    public void 주문환불_환불가능상태검증_실패() {
        // given
        given(availableRefundEvent.isRefundDateNotPassed()).willReturn(Boolean.TRUE);
        given(unavailableRefundEvent.isRefundDateNotPassed()).willReturn(Boolean.FALSE);
        given(eventAdaptor.findAllByIds(any()))
                .willReturn(List.of(availableRefundEvent, unavailableRefundEvent));
        // when
        // then
        assertThrows(
                NotRefundAvailableDateOrderException.class,
                () -> orderValidator.validAvailableRefundDate(order));
    }

    @Test
    public void 주문환불_환불가능상태검증_성공() {
        // given
        given(availableRefundEvent.isRefundDateNotPassed()).willReturn(Boolean.TRUE);
        given(eventAdaptor.findAllByIds(any())).willReturn(List.of(availableRefundEvent));
        // when
        orderValidator.validAvailableRefundDate(order);
        // then
    }

    @Test
    public void 주문_무료금액검증_성공() {
        // given
        given(order.isNeedPaid()).willReturn(Boolean.FALSE);
        // when
        orderValidator.validAmountIsFree(order);
        // then
    }

    @Test
    public void 주문_무료금액검증_실패() {
        // given
        given(order.isNeedPaid()).willReturn(Boolean.TRUE);
        // when
        // then
        assertThrows(NotFreeOrderException.class, () -> orderValidator.validAmountIsFree(order));
    }

    @Test
    public void 주문취소_상태검증_실패() {
        // given
        List<Executable> executables =
                Arrays.stream(OrderStatus.values())
                        .filter(orderStatus -> !orderValidator.isStatusCanWithDraw(orderStatus))
                        .<Executable>map(
                                orderStatus ->
                                        () -> orderValidator.validStatusCanCancel(orderStatus))
                        .toList();
        // then
        executables.forEach(
                executable -> assertThrows(CanNotCancelOrderException.class, executable));
    }

    @Test
    public void 주문취소_상태검증_성공() {
        // given
        OrderStatus confirm = OrderStatus.CONFIRM;
        OrderStatus approved = OrderStatus.APPROVED;
        // when
        orderValidator.validStatusCanCancel(confirm);
        orderValidator.validStatusCanCancel(approved);
        // then
    }

    @Test
    public void 주문환불_상태검증_실패() {
        // given
        List<Executable> executables =
                Arrays.stream(OrderStatus.values())
                        .filter(orderStatus -> !orderValidator.isStatusCanWithDraw(orderStatus))
                        .<Executable>map(
                                orderStatus ->
                                        () -> orderValidator.validStatusCanRefund(orderStatus))
                        .toList();
        // then
        executables.forEach(
                executable -> assertThrows(CanNotRefundOrderException.class, executable));
    }

    @Test
    public void 주문환불_상태검증_성공() {
        // given
        OrderStatus confirm = OrderStatus.CONFIRM;
        OrderStatus approved = OrderStatus.APPROVED;
        // when
        orderValidator.validStatusCanRefund(confirm);
        orderValidator.validStatusCanRefund(approved);
        // then
    }

    @Test
    public void 주문확인_결제대기중검증_성공() {
        // given
        OrderStatus pendingPayment = OrderStatus.PENDING_PAYMENT;
        // when
        orderValidator.validStatusCanPaymentConfirm(pendingPayment);
        // then
    }

    @Test
    public void 주문확인_결제대기중검증_실패() {
        // given
        // when
        List<Executable> executables =
                Arrays.stream(OrderStatus.values())
                        .filter(orderStatus -> !(OrderStatus.PENDING_PAYMENT == orderStatus))
                        .<Executable>map(
                                orderStatus ->
                                        () ->
                                                orderValidator.validStatusCanPaymentConfirm(
                                                        orderStatus))
                        .toList();
        // then
        executables.forEach(executable -> assertThrows(NotPendingOrderException.class, executable));
    }

    @Test
    public void 주문확인_승인대기중검증_성공() {
        // given
        OrderStatus pendingApprove = OrderStatus.PENDING_APPROVE;
        // when
        orderValidator.validStatusCanApprove(pendingApprove);
        // then
    }

    @Test
    public void 주문확인_승인대기중검증_실패() {
        // given
        // when
        List<Executable> executables =
                Arrays.stream(OrderStatus.values())
                        .filter(orderStatus -> !(OrderStatus.PENDING_APPROVE == orderStatus))
                        .<Executable>map(
                                orderStatus ->
                                        () -> orderValidator.validStatusCanApprove(orderStatus))
                        .toList();
        // then
        executables.forEach(executable -> assertThrows(NotPendingOrderException.class, executable));
    }

    @Test
    public void 주문과정중_상품옵션이_변하면_실패() {
        // given
        given(order.getOrderLineItems()).willReturn(List.of(orderLineItem));
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
                OrderItemOptionChangedException.class,
                () -> orderValidator.validOptionNotChange(order, item));
    }

    @Test
    public void 주문과정중_상품옵션이_그대로면_성공() {
        // given
        given(order.getOrderLineItems()).willReturn(List.of(orderLineItem));
        given(optionAdaptor.findAllByIds(any()))
                .willReturn(List.of(optionOfGroup1, optionOfGroup2));

        Long optionGroup1Id = 1L;
        Long optionGroup2Id = 2L;
        given(optionOfGroup1.getOptionGroupId()).willReturn(optionGroup1Id);
        given(optionOfGroup2.getOptionGroupId()).willReturn(optionGroup2Id);

        given(item.getOptionGroupIds()).willReturn(List.of(optionGroup1Id, optionGroup2Id));
        // when
        orderValidator.validOptionNotChange(order, item);
        // then
    }

    @Test
    public void 주문_티켓팅_가능시간검증_실패() {
        // given
        given(event.isTimeBeforeStartAt()).willReturn(Boolean.FALSE);
        willCallRealMethod().given(event).validateTicketingTime();
        // when
        // then
        assertThrows(
                EventTicketingTimeIsPassedException.class,
                () -> orderValidator.validTicketingTime(event));
    }

    @Test
    public void 주문_티켓팅_가능시간검증_성공() {
        // given
        given(event.isTimeBeforeStartAt()).willReturn(Boolean.TRUE);
        willCallRealMethod().given(event).validateTicketingTime();
        // when
        orderValidator.validTicketingTime(event);
        // then
    }

    @Test
    public void 주문_티켓팅_재고검증_실패() {
        // given
        willThrow(TicketItemQuantityLackException.class).given(item).validEnoughQuantity(any());
        // when
        // then
        assertThrows(
                TicketItemQuantityLackException.class,
                () -> orderValidator.validItemStockEnough(order, item));
    }

    @Test
    public void 주문_티켓팅_재고검증_성공() {
        // given
        willDoNothing().given(item).validEnoughQuantity(any());
        // when
        orderValidator.validItemStockEnough(order, item);
        // then
    }

    @Test
    public void 주문_티켓팅_이벤트_상태검증_성공() {
        // given
        willDoNothing().given(event).validateStatusOpen();
        // when
        orderValidator.validEventIsOpen(event);
        // then
    }

    @Test
    public void 주문_티켓팅_이벤트_상태검증_실패() {
        // given
        willThrow(EventNotOpenException.class).given(event).validateStatusOpen();
        // when
        // then
        assertThrows(EventNotOpenException.class, () -> orderValidator.validEventIsOpen(event));
    }

    @Test
    public void 주문_아이템_한종류가아니면_실패() {
        // given
        given(order.getDistinctItemIds()).willReturn(List.of(1L, 2L));
        // then
        assertThrows(
                OrdeItemNotOneTypeException.class,
                () -> orderValidator.validItemKindIsOneType(order));
    }

    @Test
    public void 주문_아이템_한종류면_성공() {
        // given
        given(order.getDistinctItemIds()).willReturn(List.of(2L));
        // then
        orderValidator.validItemKindIsOneType(order);
    }

    @Test
    public void 주문_아이템_구매갯수제한_실패() {
        // given
        given(order.getTotalQuantity()).willReturn(3L);
        willThrow(TicketPurchaseLimitException.EXCEPTION).given(item).validPurchaseLimit(any());
        // then
        assertThrows(
                TicketPurchaseLimitException.class,
                () -> orderValidator.validItemPurchaseLimit(order, item));
    }
}
