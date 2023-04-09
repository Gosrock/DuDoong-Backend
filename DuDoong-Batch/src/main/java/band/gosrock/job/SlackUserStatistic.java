package band.gosrock.job;


import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.helper.slack.SlackUserNotificationSender;
import band.gosrock.parameter.DateJobParameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class SlackUserStatistic {

    private static final String JOB_NAME = "슬랙유저통계";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final SlackUserNotificationSender slackSender;

    private final UserAdaptor userAdaptor;
    private final DateJobParameter dateJobParameter;

    @Bean(BEAN_PREFIX + "dateJobParameter")
    @JobScope
    public DateJobParameter dateJobParameter() {
        return new DateJobParameter();
    }

    @Bean(JOB_NAME)
    public Job slackUserStatisticJob() {
        return jobBuilderFactory
                .get(JOB_NAME)
                .preventRestart()
                // 파라미터로 version 정보를 넘겨주면 개발환경에서 계속 돌려볼수 있다.
                // 젠킨스의 경우 매번 달라지는 환경변수인 BUILD_ID 를 제공한다.
                // https://jojoldu.tistory.com/487 맨밑 글 참조
                .start(userStatisticStep())
                .build();
    }

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    public Step userStatisticStep() {
        return stepBuilderFactory
                .get(BEAN_PREFIX + "step")
                .tasklet(
                        (contribution, chunkContext) -> {
                            log.info(">>>>> 슬랙 유저 통계 스탭");
                            LocalDate date = dateJobParameter.getDate();
                            LocalDateTime today = date.atTime(LocalTime.MAX);
                            LocalDateTime yesterday = today.minusDays(1L);

                            Long todayCount = userAdaptor.countNormalUserCreatedBefore(today);
                            Long yesterdayCount =
                                    userAdaptor.countNormalUserCreatedBefore(yesterday);

                            slackSender.execute(date, todayCount, yesterdayCount);
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
