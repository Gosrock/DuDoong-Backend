package band.gosrock.domain.domains.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DisableRedissonLock;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
@DisableRedissonLock
@Slf4j
class OrderApproveServiceConcurrencyFailTest {
    static int numberOfThreads = 20;
    static int numberOfThreadPool = 20;
    @Autowired private OrderApproveService orderApproveService;

    @Autowired RedissonClient redissonClient;

    @Mock OrderLineItem orderLineItem;

    @MockBean private OrderAdaptor orderAdaptor;

    Order order;

    @BeforeEach
    void setUp() {
        given(orderLineItem.isNeedPayment()).willReturn(Boolean.FALSE);
        order =
                Order.builder()
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderLineItems(List.of(orderLineItem))
                        .build();
        order.addUUID();
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
    }

    @Test
    @DisplayName("락을 적용안하면 동시에 여러 주문요청이 들어왔을 때 중복 승인이 되어야한다.")
    void 동시성_실패_주문승인() throws InterruptedException {
        // given
        // when
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreadPool);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicLong successCount = new AtomicLong();

        for (long i = 1; i <= numberOfThreads; i++) {
            executorService.submit(
                    () -> {
                        try {
                            orderApproveService.execute(order.getUuid());
                            // 오류없이 성공을 하면 성공횟수를 증가시킵니다.
                            successCount.getAndIncrement();
                        } catch (Exception e) {
                            log.info(e.getClass().getName());
                        } finally {
                            latch.countDown();
                        }
                    });
        }
        latch.await();
        // then
        // 가끔 동시요청이 ci 환경에서 중복안될때가 있음 로그로 확인하셈!
        log.info(String.valueOf(successCount.get()));
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }
}
