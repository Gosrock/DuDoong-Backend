package band.gosrock.job

import band.gosrock.domain.common.alarm.SettlementKakaoTalkAlarm
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper
import band.gosrock.parameter.EventJobParameter
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventSettlementAlimTalkToHost(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
    private val ncpHelper: NcpHelper,
    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private val eventJobParameter: EventJobParameter,
) {
    private val log = LoggerFactory.getLogger(EventSettlementAlimTalkToHost::class.java)

    companion object {
        private const val JOB_NAME = "이벤트정산_알림톡발송_호스트"
        const val BEAN_PREFIX = "${JOB_NAME}_"
    }

    @Bean(BEAN_PREFIX + "eventJobParameter")
    @JobScope
    fun eventJobParameter(): EventJobParameter = EventJobParameter(eventAdaptor)

    @Bean(JOB_NAME)
    fun slackUserStatisticJob(): Job =
        jobBuilderFactory.get(JOB_NAME).preventRestart().start(eventSettlement()).build()

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    fun eventSettlement(): Step =
        stepBuilderFactory
            .get(BEAN_PREFIX + "step")
            .tasklet { _, _ ->
                log.info(">>>>> 정산서 전송 안내 알림톡 스탭")
                val event = eventJobParameter.getEvent()
                val host = hostAdaptor.findById(event.hostId!!)
                val masterUser = userAdaptor.queryUser(host.masterUserId!!)

                val to = masterUser.profile!!.phoneNumberVo!!.getNaverSmsToNumber()
                val content = SettlementKakaoTalkAlarm.creationOf(masterUser.profile!!.name!!)

                ncpHelper.sendSettlementNcpAlimTalk(
                    to,
                    SettlementKakaoTalkAlarm.creationTemplateCode(),
                    content,
                    SettlementKakaoTalkAlarm.creationHeaderOf(),
                    masterUser.profile!!.email!!,
                    event.eventBasic!!.name!!,
                )
                RepeatStatus.FINISHED
            }
            .build()
}
