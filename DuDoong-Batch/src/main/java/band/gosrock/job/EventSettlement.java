package band.gosrock.job;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.infrastructure.outer.api.tossPayments.client.SettlementClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse;
import band.gosrock.parameter.DateJobParameter;
import band.gosrock.parameter.EventJobParameter;
import band.gosrock.slack.SlackSender;
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
public class EventSettlement {

    private static final String JOB_NAME = "이벤트정산";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final SlackSender slackSender;
    private final EventAdaptor eventAdaptor;

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

    private final UserAdaptor userAdaptor;
    private final DateJobParameter dateJobParameter;

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
                            // 시작 날짜. ( 이벤트 생성 시간 )
                            LocalDate startAt = event.getCreatedAt().toLocalDate();
                            // 끝나는 날짜.
                            LocalDate endAt = event.getEndAt().toLocalDate();
                            // 데이터가 실제로 들어가야만 있음.. 테스트 코드로 돌려야함.
                            List<SettlementResponse> settlementList =
                                    settlementClient.execute(startAt, endAt, "soldDate", 1, 10000);
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
