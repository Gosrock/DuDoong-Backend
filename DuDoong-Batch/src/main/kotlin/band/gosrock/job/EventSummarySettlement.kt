package band.gosrock.job

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.settlement.adaptor.EventSettlementAdaptor
import band.gosrock.domain.domains.settlement.service.EventSettlementDomainService
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

/** 이벤트 정산 수수료 , 내역등을 요약해서 저장합니다. */
@Configuration
class EventSummarySettlement(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private val eventJobParameter: EventJobParameter,
    private val eventSettlementAdaptor: EventSettlementAdaptor,
    private val eventSettlementDomainService: EventSettlementDomainService,
) {
    private val log = LoggerFactory.getLogger(EventSummarySettlement::class.java)

    companion object {
        private const val JOB_NAME = "이벤트정산요약"
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
                val event = eventJobParameter.getEvent()
                val eventId = event.id!!
                eventSettlementAdaptor.deleteByEventId(eventId)
                val orders = orderAdaptor.findByEventId(eventId)
                eventSettlementDomainService.generateEventSettlement(eventId, orders)
                RepeatStatus.FINISHED
            }
            .build()
}
