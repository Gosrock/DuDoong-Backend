package band.gosrock.job

import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.helper.slack.SlackUserNotificationSender
import band.gosrock.parameter.DateJobParameter
import java.time.LocalTime
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
class SlackUserStatistic(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val slackSender: SlackUserNotificationSender,
    private val userAdaptor: UserAdaptor,
    private val dateJobParameter: DateJobParameter,
) {
    private val log = LoggerFactory.getLogger(SlackUserStatistic::class.java)

    companion object {
        private const val JOB_NAME = "슬랙유저통계"
        private const val BEAN_PREFIX = "${JOB_NAME}_"
    }

    @Bean(BEAN_PREFIX + "dateJobParameter")
    @JobScope
    fun dateJobParameter(): DateJobParameter = DateJobParameter()

    @Bean(JOB_NAME)
    fun slackUserStatisticJob(): Job =
        jobBuilderFactory
            .get(JOB_NAME)
            .preventRestart()
            // 파라미터로 version 정보를 넘겨주면 개발환경에서 계속 돌려볼수 있다.
            // 젠킨스의 경우 매번 달라지는 환경변수인 BUILD_ID 를 제공한다.
            // https://jojoldu.tistory.com/487 맨밑 글 참조
            .start(userStatisticStep())
            .build()

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    fun userStatisticStep(): Step =
        stepBuilderFactory
            .get(BEAN_PREFIX + "step")
            .tasklet { _, _ ->
                log.info(">>>>> 슬랙 유저 통계 스탭")
                val date = dateJobParameter.getDate()
                val today = date.atTime(LocalTime.MAX)
                val yesterday = today.minusDays(1L)

                val todayCount = userAdaptor.countNormalUserCreatedBefore(today)
                val yesterdayCount = userAdaptor.countNormalUserCreatedBefore(yesterday)

                slackSender.execute(date, todayCount, yesterdayCount)
                RepeatStatus.FINISHED
            }
            .build()
}
