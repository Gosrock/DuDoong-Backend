package band.gosrock.job

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.settlement.adaptor.TransactionSettlementAdaptor
import band.gosrock.domain.domains.settlement.domain.TransactionSettlement
import band.gosrock.infrastructure.outer.api.tossPayments.client.SettlementClient
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse
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

/** 토스페이먼츠 거래 내역들을 이벤트별로 취합 후 저장합니다. */
@Configuration
class EventTransactionSettlement(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private val eventJobParameter: EventJobParameter,
    private val settlementClient: SettlementClient,
    private val transactionSettlementAdaptor: TransactionSettlementAdaptor,
) {
    private val log = LoggerFactory.getLogger(EventTransactionSettlement::class.java)

    companion object {
        private const val JOB_NAME = "이벤트거래정산"
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
                // 멱등성 유지하기위해 저장된 정산 목록에서 지움 ( 실제로 다시만들일은 없을듯...? )
                transactionSettlementAdaptor.deleteByEventId(eventId)
                // 정산 정보 저장
                transactionSettlementAdaptor.saveAll(getTransactionSettlements(event))
                RepeatStatus.FINISHED
            }
            .build()

    private fun getPaymentOrderUUIDs(eventId: Long): List<String> {
        val orders = orderAdaptor.findByEventId(eventId)
        return orders
            .filter { it.isPaid() }
            .map { it.pgPaymentInfo.paymentKey }
    }

    private fun getTransactionSettlements(event: Event): List<TransactionSettlement> {
        val eventId = event.id!!
        val paymentOrderUuids = getPaymentOrderUUIDs(eventId)
        val settlements = getTossPaymentsSettlementData(event)

        return settlements
            .filter { paymentOrderUuids.contains(it.paymentKey) }
            .map { TransactionSettlement.of(eventId, it) }
    }

    // 토스페이먼츠에서 시작일 종료일 매출일 기준 정산액을 조회합니다.
    private fun getTossPaymentsSettlementData(event: Event): List<SettlementResponse> {
        val startAt = event.createdAtKt().toLocalDate()
        val endAt = event.getEndAt()!!.toLocalDate()
        return settlementClient.execute(startAt, endAt, "soldDate", 1, 10000)
    }
}
