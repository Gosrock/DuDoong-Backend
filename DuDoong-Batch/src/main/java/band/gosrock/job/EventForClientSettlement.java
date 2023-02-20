package band.gosrock.job;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
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
public class EventForClientSettlement {

    private static final String JOB_NAME = "이벤트클라이언트정산";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EventAdaptor eventAdaptor;
    private final OrderAdaptor orderAdaptor;

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

    private final SettlementClient settlementClient;

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
                            List<Order> orders = orderAdaptor.findByEventId(event.getId());
                            // 오더중에 토스 페이먼츠 로결제를 진행한 목록을 추출.
                            List<String> paymentOrderUuids =
                                    orders.stream()
                                            .filter(
                                                    order ->
                                                            order.getOrderStatus()
                                                                    == OrderStatus.CONFIRM)
                                            .map(Order::getUuid)
                                            .toList();

                            // 시작 날짜. ( 이벤트 생성 시간 )
                            LocalDate startAt = event.getCreatedAt().toLocalDate();
                            // 끝나는 날짜.
                            LocalDate endAt = event.getEndAt().toLocalDate();
                            // 데이터가 실제로 들어가야만 있음.. 테스트 코드로 돌려야함.
                            List<SettlementResponse> settlements =
                                    settlementClient.execute(startAt, endAt, "soldDate", 1, 10000);

                            // 이벤트와 관련된 정산 객체 집합.
                            List<SettlementResponse> eventSettlementResponses =
                                    settlements.stream()
                                            .filter(
                                                    settlementResponse ->
                                                            paymentOrderUuids.contains(
                                                                    settlementResponse
                                                                            .getOrderId()))
                                            .toList();

                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
