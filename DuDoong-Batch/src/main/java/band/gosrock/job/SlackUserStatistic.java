package band.gosrock.job;

import static band.gosrock.job.BatchStatic.JOB;
import static band.gosrock.job.BatchStatic.STEP;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
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

    @Bean(name = JOB + NAME)
    public Job simpleJob() {
        return jobBuilderFactory.get(NAME)
            .start(exampleStep())
            .build();
    }

    @Bean(name = STEP + NAME)
    public Step exampleStep() {
        return stepBuilderFactory.get(NAME)
            .tasklet((contribution, chunkContext) -> {
                log.info(">>>>> 슬랙 유저 통계 잡");
                return RepeatStatus.FINISHED;
            })
            .build();
    }
}
