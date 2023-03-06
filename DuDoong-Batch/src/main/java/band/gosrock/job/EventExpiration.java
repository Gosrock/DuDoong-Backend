package band.gosrock.job;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.parameter.DateTimeJobParameter;
import band.gosrock.slack.SlackEventExpirationSender;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class EventExpiration {

    private static final String JOB_NAME = "이벤트_자동만료";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final SlackEventExpirationSender slackEventExpirationSender;
    private final EventService eventService;

    @Bean(BEAN_PREFIX + "dateTimeJobParameter")
    @JobScope
    public DateTimeJobParameter dateTimeJobParameter() {
        return new DateTimeJobParameter();
    }

    @Bean(JOB_NAME)
    public Job eventExpirationJob() {
        return jobBuilderFactory
                .get(JOB_NAME)
                .preventRestart()
                .start(eventExpirationStep())
                .build();
    }

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    public Step eventExpirationStep() {
        return stepBuilderFactory
                .get(BEAN_PREFIX + "step")
                .tasklet(
                        (contribution, chunkContext) -> {
                            log.info(">>>>> 이벤트 자동 만료 작업 실행");
                            LocalDateTime time = dateTimeJobParameter().getTime();
                            List<Event> events = eventService.closeExpiredEventsEndAtBefore(time);
                            slackEventExpirationSender.execute(time, events);
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
