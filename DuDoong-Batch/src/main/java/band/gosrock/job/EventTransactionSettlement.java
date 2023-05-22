package band.gosrock.job;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.settlement.adaptor.TransactionSettlementAdaptor;
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement;
import band.gosrock.infrastructure.outer.api.tossPayments.client.SettlementClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse;
import band.gosrock.parameter.EventJobParameter;
import java.time.LocalDate;
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

/** 토스페이먼츠 거래 내역들을 이벤트별로 취합 후 저장합니다. */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class EventTransactionSettlement {

    private static final String JOB_NAME = "이벤트거래정산";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EventAdaptor eventAdaptor;
    private final OrderAdaptor orderAdaptor;

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

    private final SettlementClient settlementClient;

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
                            // 멱등성 유지하기위해 저장된 정산 목록에서 지움 ( 실제로 다시만들일은 없을듯...? )
                            transactionSettlementAdaptor.deleteByEventId(eventId);

                            // 정산 정보 저장
                            transactionSettlementAdaptor.saveAll(getTransactionSettlements(event));

                            return RepeatStatus.FINISHED;
                        })
                .build();
    }

    private List<String> getPaymentOrderUUIDs(Long eventId) {
        List<Order> orders = orderAdaptor.findByEventId(eventId);
        return orders.stream()
                .filter(Order::isPaid)
                .map(order -> order.getPgPaymentInfo().getPaymentKey())
                .toList();
    }

    private List<TransactionSettlement> getTransactionSettlements(Event event) {
        Long eventId = event.getId();
        List<String> paymentOrderUuids = getPaymentOrderUUIDs(eventId);
        List<SettlementResponse> settlements = getTossPaymentsSettlementData(event);

        return settlements.stream()
                .filter(
                        settlementResponse ->
                                paymentOrderUuids.contains(settlementResponse.getPaymentKey()))
                .map(settlementResponse -> TransactionSettlement.of(eventId, settlementResponse))
                .toList();
    }

    // 토스페이먼츠에서 시작일 종료일 매출일 기준 정산액을 조회합니다.
    private List<SettlementResponse> getTossPaymentsSettlementData(Event event) {
        LocalDate startAt = event.getCreatedAt().toLocalDate();
        LocalDate endAt = event.getEndAt().toLocalDate();
        return settlementClient.execute(startAt, endAt, "soldDate", 1, 10000);
    }
}
