package band.gosrock.job

import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.helper.slack.SlackEventExpirationSender
import band.gosrock.parameter.DateTimeJobParameter
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventExpiration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val slackEventExpirationSender: SlackEventExpirationSender,
    private val eventService: EventService,
) {
    private val log = LoggerFactory.getLogger(EventExpiration::class.java)

    companion object {
        private const val JOB_NAME = "이벤트_자동만료"
        private const val BEAN_PREFIX = "${JOB_NAME}_"
    }

    @Bean(BEAN_PREFIX + "dateTimeJobParameter")
    @JobScope
    fun dateTimeJobParameter(): DateTimeJobParameter = DateTimeJobParameter()

    @Bean(JOB_NAME)
    fun eventExpirationJob(): Job =
        jobBuilderFactory
            .get(JOB_NAME)
            .preventRestart()
            .start(eventExpirationStep())
            .build()

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    fun eventExpirationStep(): Step =
        stepBuilderFactory
            .get(BEAN_PREFIX + "step")
            .tasklet { _, _ ->
                log.info(">>>>> 이벤트 자동 만료 작업 실행")
                val time = dateTimeJobParameter().getTime()
                val events = eventService.closeExpiredEventsEndAtBefore(time)
                slackEventExpirationSender.execute(time, events)
                RepeatStatus.FINISHED
            }
            .build()
}
