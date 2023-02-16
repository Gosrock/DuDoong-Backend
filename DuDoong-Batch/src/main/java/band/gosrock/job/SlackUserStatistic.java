package band.gosrock.job;

import static band.gosrock.job.BatchStatic.JOB;
import static band.gosrock.job.BatchStatic.STEP;

import band.gosrock.slack.SlackSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SlackUserStatistic {

    private final String NAME = "슬랙유저통계";
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final SlackSender slackSender;
    @Bean(name = JOB + NAME)
    public Job simpleJob() {
        return jobBuilderFactory.get(NAME)
            .start(userStatisticStep())
            .build();
    }

    @Bean(name = STEP + NAME)
    public Step userStatisticStep() {
        return stepBuilderFactory.get(STEP + NAME)
            .tasklet((contribution, chunkContext) -> {
                log.info(">>>>> 슬랙 유저 통계 스탭");
                slackSender.execute(10L);
                return RepeatStatus.FINISHED;
            })
            .build();
    }
}
