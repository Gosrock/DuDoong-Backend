package band.gosrock.job;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.settlement.adaptor.EventSettlementAdaptor;
import band.gosrock.domain.domains.settlement.adaptor.TransactionSettlementAdaptor;
import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement;
import band.gosrock.parameter.EventJobParameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class EventForClientSettlement {

    private static final String JOB_NAME = "이벤트클라이언트정산";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EventAdaptor eventAdaptor;
    private final OrderAdaptor orderAdaptor;

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

    private final EventSettlementAdaptor eventSettlementAdaptor;

    private final TransactionSettlementAdaptor transactionSettlementAdaptor;

    @Bean(BEAN_PREFIX + "eventJobParameter")
    @JobScope
    public EventJobParameter eventJobParameter() {
        return new EventJobParameter(eventAdaptor);
    }

    @Bean(JOB_NAME)
    public Job slackUserStatisticJob() {
        return jobBuilderFactory.get(JOB_NAME).preventRestart().start(eventSettlement()).build();
    }

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    public Step eventSettlement() {
        return stepBuilderFactory
                .get(BEAN_PREFIX + "step")
                .tasklet(
                        (contribution, chunkContext) -> {
                            Event event = eventJobParameter.getEvent();
                            Long eventId = event.getId();
                            List<Order> orders = orderAdaptor.findByEventId(eventId);
                            // 오더중에 승인 주문중 두둥 총
                            Money approveOrderTotalSales =
                                    orders.stream()
                                            .filter(
                                                    order ->
                                                            order.getOrderStatus()
                                                                    == OrderStatus.APPROVED)
                                            .map(Order::getTotalPaymentPrice)
                                            .reduce(Money.ZERO, Money::plus);

                            // 오더중에 토스 페이먼츠 로결제를 진행한 목록을 추출.
                            Money paymentOrderTotalSales =
                                    orders.stream()
                                            .filter(
                                                    order ->
                                                            order.getOrderStatus()
                                                                    == OrderStatus.CONFIRM)
                                            .map(Order::getTotalPaymentPrice)
                                            .reduce(Money.ZERO, Money::plus);
                            // 오더중에 토스 페이먼츠로 결제를 진행한 목록중 쿠폰 할인 금액.
                            Money paymentOrderDiscountAmount =
                                    orders.stream()
                                            .filter(
                                                    order ->
                                                            order.getOrderStatus()
                                                                    == OrderStatus.CONFIRM)
                                            .map(Order::getTotalDiscountPrice)
                                            .reduce(Money.ZERO, Money::plus);

                            // 이벤트 트랜잭션 저장 목록 중에서 결제 대행 수수료 저장
                            List<TransactionSettlement> transactionSettlements =
                                    transactionSettlementAdaptor.findByEventId(eventId);
                            Money paymentAmount =
                                    transactionSettlements.stream()
                                            .map(TransactionSettlement::getPaymentAmount)
                                            .reduce(Money.ZERO, Money::plus);

                            transactionSettlements.stream()
                                    .map(TransactionSettlement::getSettlementAmount)
                                    .reduce(Money.ZERO, Money::plus);
                            // 중개 수수료 계산 공식? 그냥 정액으로..

                            // 최종 정산 금액 계산.

                            EventSettlement eventSettlement =
                                    eventSettlementAdaptor.upsertByEventId(eventId);
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
