package band.gosrock.domain.domains.order.domain.validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.domain.Order;
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
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
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

    @Mock Event availableRefundEvent;
    @Mock Event unavailableRefundEvent;
    @Mock EventAdaptor eventAdaptor;
    @Mock TicketItemAdaptor ticketItemAdaptor;

    OrderValidator orderValidator;

    @BeforeEach
    void setUp() {
        orderValidator = new OrderValidator(eventAdaptor, ticketItemAdaptor);
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
}
