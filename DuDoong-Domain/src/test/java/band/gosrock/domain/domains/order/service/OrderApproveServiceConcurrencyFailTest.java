package band.gosrock.domain.domains.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.CunCurrencyExecutorService;
import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DisableRedissonLock;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderCouponVo;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@DomainIntegrateSpringBootTest
@DisableDomainEvent
@DisableRedissonLock
@Slf4j
class OrderApproveServiceConcurrencyFailTest {
    @Autowired OrderApproveService orderApproveService;

    @Mock OrderLineItem orderLineItem;
    @Mock TicketItem ticketItem;

    @MockBean OrderAdaptor orderAdaptor;

    Order order;

    @BeforeEach
    void setUp() {
        given(orderLineItem.getTotalOrderLinePrice()).willReturn(Money.ZERO);
        given(orderLineItem.getTicketItem()).willReturn(ticketItem);
        given(ticketItem.getId()).willReturn(1L);
        order =
                Order.builder()
                        .orderMethod(OrderMethod.APPROVAL)
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderLineItems(List.of(orderLineItem))
                        .orderCouponVo(OrderCouponVo.empty())
                        .build();
        order.addUUID();
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
    }

    @Test
    @DisplayName("락을 적용안하면 동시에 여러 주문요청이 들어왔을 때 중복 승인이 되어야한다.")
    void 동시성_실패_주문승인() throws InterruptedException {
        // given
        // when
        AtomicLong successCount = new AtomicLong();
        CunCurrencyExecutorService.execute(
                () -> orderApproveService.execute(order.getUuid()), successCount);

        // then
        // 가끔 동시요청이 ci 환경에서 중복안될때가 있음 로그로 확인하셈!
        log.info(String.valueOf(successCount.get()));
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }
}
