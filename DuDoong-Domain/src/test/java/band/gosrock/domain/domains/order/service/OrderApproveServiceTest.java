package band.gosrock.domain.domains.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderApproveServiceTest {


    @Mock
    private OrderAdaptor orderAdaptor;

    @Mock
    private Order order;

    @Test
    @DisplayName("주문승인_승인로직_한번만_호출해야한다.")
    void 주문승인_승인로직_한번만_호출(){
        //given
        willDoNothing().given(order).approve();
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
        OrderApproveService orderApproveService = new OrderApproveService(orderAdaptor);


        //when
        orderApproveService.execute("uuid");

        //then
        then(order).should(times(1)).approve();
    }
}