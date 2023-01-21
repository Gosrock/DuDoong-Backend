package band.gosrock.domain.domains.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.domain.DomainIntegrateProfileResolver;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.DomainIntegrateTestConfig;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderMethod;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


@DomainIntegrateSpringBootTest
@Slf4j
class OrderApproveServiceTest {

    static int numberOfThreads = 10;
    static int numberOfThreadPool = 10;

    @Autowired
    private OrderApproveService orderApproveService;

    @Mock
    OrderLineItem orderLineItem;

    @Autowired
    RedissonClient redissonClient;

    Order pendingApproveOrder;

    @MockBean
    private OrderAdaptor orderAdaptor;

    @BeforeEach
    void setUp() {
        // 승인 방식의 결제를 지정합니다.
        given(orderLineItem.isNeedPayment()).willReturn(Boolean.FALSE);
        Order order = Order.builder().orderLineItems(List.of(orderLineItem))
            .orderMethod(OrderMethod.APPROVAL)
            .userId(1L)
            .orderName("주문이름")
            .orderStatus(OrderStatus.PENDING_APPROVE).build();
        order.addUUID();
        given(orderAdaptor.findByOrderUuid(any())).willReturn(order);
        pendingApproveOrder = order;
    }

//    @AfterEach
//    void teardown() {
//        System.out.println("teardown");
//
//        couponRepository.deleteAll();
//        campaignRepository.deleteAll();
//    }

    @Test
    @DisplayName("동시에 주문 승인 요청이 와도 하나의 요청만 성공해야한다.")
    void 동시성_주문승인() throws InterruptedException {
        //given
        //when
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong successCount = new AtomicLong();

        for (long i= 1L; i<=numberOfThreads; i++) {
//            final Long userId = i;
            executorService.submit(() -> {
                try {
                    orderApproveService.execute(pendingApproveOrder.getUuid());
                    // 오류없이 성공을 하면 성공횟수를 증가시킵니다.
                    successCount.getAndIncrement();
                } catch (DuDoongCodeException e){
                    log.info(e.getClass().getName());
                }finally{
                    latch.countDown();
                }
            });
        }
        latch.await();
        //then
        assertThat(successCount.get()).isEqualTo(1);
    }
}