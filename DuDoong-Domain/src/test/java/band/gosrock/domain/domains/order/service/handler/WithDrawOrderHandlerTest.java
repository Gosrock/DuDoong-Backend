package band.gosrock.domain.domains.order.service.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.service.WithdrawPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WithDrawOrderHandlerTest {

    @Mock OrderAdaptor orderAdaptor;

    @Mock WithdrawPaymentService withdrawPaymentService;

    @Mock WithDrawOrderEvent withDrawOrderEvent;

    @Mock Order order;

    @Test
    public void 결제된_주문이_아니면_토스로_철회요청을_보내지_않는다() {
        // given
        WithDrawOrderHandler withDrawOrderHandler =
                new WithDrawOrderHandler(withdrawPaymentService, orderAdaptor);
        given(order.isPaid()).willReturn(Boolean.FALSE);
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
        // when
        withDrawOrderHandler.handleWithDrawOrderEvent(withDrawOrderEvent);
        // then
        then(withdrawPaymentService).should(times(0)).execute(any(), any(), any());
    }

    @Test
    public void 결제된_주문이면_토스로_철회요청을_보낸다() {
        // given
        WithDrawOrderHandler withDrawOrderHandler =
                new WithDrawOrderHandler(withdrawPaymentService, orderAdaptor);
        given(order.isPaid()).willReturn(Boolean.TRUE);
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
        given(withDrawOrderEvent.getOrderStatus()).willReturn(OrderStatus.CANCELED);
        // when
        withDrawOrderHandler.handleWithDrawOrderEvent(withDrawOrderEvent);
        // then
        then(withdrawPaymentService).should(times(1)).execute(any(), any(), any());
    }
}
