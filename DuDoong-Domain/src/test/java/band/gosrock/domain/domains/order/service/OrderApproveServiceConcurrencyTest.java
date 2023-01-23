package band.gosrock.domain.domains.order.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import band.gosrock.domain.CunCurrencyExecutorService;
import band.gosrock.domain.DisableDomainEvent;
import band.gosrock.domain.DomainIntegrateSpringBootTest;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
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

    Order order;

    @BeforeEach
    void setUp() {
        given(orderLineItem.isNeedPaid()).willReturn(Boolean.FALSE);
        order =
                Order.builder()
                        .orderMethod(OrderMethod.APPROVAL)
                        .orderStatus(OrderStatus.PENDING_APPROVE)
                        .orderLineItems(List.of(orderLineItem))
                        .build();
        order.addUUID();
        // https://stackoverflow.com/questions/11785498/simulate-first-call-fails-second-call-succeeds
        // 첫 요청엔 성공하도록
        // 그 이후 두번째부턴 실패하도록 하고싶다면? ( 그냥 예시로 남긴거임 )
        //        BDDStubber bddStubber = willDoNothing();
        //        for(int i =0 ; i <= numberOfThreads ; i++){
        //            bddStubber.willThrow(new
        // DuDoongCodeException(OrderErrorCode.ORDER_NOT_PENDING));
        //        }
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
