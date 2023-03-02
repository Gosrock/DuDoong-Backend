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
                            // 멱등성 유지하기위해
                            // 저장된 정산 목록에서 지움
                            transactionSettlementAdaptor.deleteByEventId(eventId);

                            List<Order> orders = orderAdaptor.findByEventId(eventId);
                            // 오더중에 토스 페이먼츠 로결제를 진행한 목록을 추출.
                            // 해당이벤트의 모든 주문 내역을 불러와야함.
                            // 토스에 환불진행되는 입금 건도 있음
                            List<String> paymentOrderUuids =
                                    orders.stream()
                                            .filter(order -> order.isPaid())
                                            .map(order -> order.getPgPaymentInfo().getPaymentKey())
                                            .toList();

                            // 시작 날짜. ( 이벤트 생성 시간 )
                            LocalDate startAt = event.getCreatedAt().toLocalDate();
                            // 끝나는 날짜.
                            LocalDate endAt = event.getEndAt().toLocalDate();
                            // 데이터가 실제로 들어가야만 있음.. 테스트 코드로 돌려야함.
                            List<SettlementResponse> settlements =
                                    settlementClient.execute(startAt, endAt, "soldDate", 1, 10000);

                            // 이벤트와 관련된 정산 객체 집합.
                            List<TransactionSettlement> transactionSettlements =
                                    settlements.stream()
                                            .filter(
                                                    settlementResponse ->
                                                            paymentOrderUuids.contains(
                                                                    settlementResponse
                                                                            .getPaymentKey()))
                                            .map(
                                                    settlementResponse ->
                                                            TransactionSettlement.of(
                                                                    eventId, settlementResponse))
                                            .toList();
                            // 정산 정보 저장.
                            transactionSettlementAdaptor.saveAll(transactionSettlements);

                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
