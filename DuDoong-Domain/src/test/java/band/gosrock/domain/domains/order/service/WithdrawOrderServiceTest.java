package band.gosrock.domain.domains.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

import band.gosrock.domain.CunCurrencyExecutorService;
import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@DomainIntegrateSpringBootTest
@DisableDomainEvent
@Slf4j
class WithdrawOrderServiceTest {

    @Autowired WithdrawOrderService withdrawOrderService;

    @MockBean OrderAdaptor orderAdaptor;
    @Mock OrderLineItem orderLineItem;

    @MockBean OrderValidator orderValidator;
    Order order;
    static Long userId = 1L;

    @BeforeEach
    void setUp() {
        order =
                Order.builder()
                        .userId(userId)
                        .orderStatus(OrderStatus.CONFIRM)
                        .orderLineItems(List.of(orderLineItem))
                        .build();
        order.addUUID();
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
        given(orderLineItem.getTotalOrderLinePrice()).willReturn(Money.ZERO);
        willDoNothing().given(orderValidator).validAvailableRefundDate(any());
    }

    @Test
    void 동시성_주문철회_취소케이스() throws InterruptedException {
        // given
        AtomicLong successCount = new AtomicLong();
        // when
        CunCurrencyExecutorService.execute(
                () -> withdrawOrderService.cancelOrder(order.getUuid()), successCount);

        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 동시성_주문철회_환불케이스() throws InterruptedException {
        // given
        AtomicLong successCount = new AtomicLong();
        // when
        CunCurrencyExecutorService.execute(
                () -> withdrawOrderService.refundOrder(order.getUuid(), userId), successCount);

        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }
}
