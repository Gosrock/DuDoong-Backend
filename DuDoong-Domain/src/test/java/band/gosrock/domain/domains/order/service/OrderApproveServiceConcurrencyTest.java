package band.gosrock.domain.domains.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willDoNothing;

import band.gosrock.domain.CunCurrencyExecutorService;
import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@DomainIntegrateSpringBootTest
@DisableDomainEvent
@Slf4j
class OrderApproveServiceConcurrencyTest {

    @Autowired private OrderApproveService orderApproveService;

    @Autowired RedissonClient redissonClient;

    @Mock OrderLineItem orderLineItem;
    @MockBean private OrderAdaptor orderAdaptor;
    @MockBean private OrderValidator orderValidator;

    Order order;

    @BeforeEach
    void setUp() {
        given(orderLineItem.getTotalOrderLinePrice()).willReturn(Money.ZERO);
        order =
                Order.builder()
                        .orderMethod(OrderMethod.APPROVAL)
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderLineItems(List.of(orderLineItem))
                        .build();
        order.addUUID();
        willDoNothing().given(orderValidator).validCanDone(any());
        willCallRealMethod().given(orderValidator).validCanApproveOrder(any());
        willCallRealMethod().given(orderValidator).validStatusCanApprove(any());
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
    }

    @Test
    @DisplayName("동시에 주문 승인 요청이 와도 하나의 요청만 성공해야한다.")
    void 동시성_주문승인() throws InterruptedException {
        // given
        // when
        AtomicLong successCount = new AtomicLong();
        CunCurrencyExecutorService.execute(
                () -> orderApproveService.execute(order.getUuid()), successCount);
        // then
        assertThat(successCount.get()).isEqualTo(1);
    }
}
