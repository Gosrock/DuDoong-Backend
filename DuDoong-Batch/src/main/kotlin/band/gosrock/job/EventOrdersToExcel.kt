package band.gosrock.job

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.helper.excel.ExcelOrderDto
import band.gosrock.helper.excel.ExcelOrderHelper
import band.gosrock.infrastructure.config.s3.S3PrivateFileService
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

/** 공연 관련 주문 목록을 엑셀화 하여 프라이빗한 S3에 저장합니다. */
@Configuration
class EventOrdersToExcel(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val excelOrderHelper: ExcelOrderHelper,
    private val eventAdaptor: EventAdaptor,
    private val orderAdaptor: OrderAdaptor,
    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private val eventJobParameter: EventJobParameter,
    private val s3PrivateFileUploadService: S3PrivateFileService,
) {
    private val log = LoggerFactory.getLogger(EventOrdersToExcel::class.java)

    companion object {
        private const val JOB_NAME = "이벤트주문목록_엑셀업로드"
        const val BEAN_PREFIX = "${JOB_NAME}_"
    }

    @Bean(BEAN_PREFIX + "eventJobParameter")
    @JobScope
    fun eventJobParameter(): EventJobParameter = EventJobParameter(eventAdaptor)

    @Bean(JOB_NAME)
    fun slackUserStatisticJob(): Job =
        jobBuilderFactory.get(JOB_NAME).preventRestart().start(userStatisticStep()).build()

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    fun userStatisticStep(): Step =
        stepBuilderFactory
            .get(BEAN_PREFIX + "step")
            .tasklet { _, _ ->
                val event = eventJobParameter.getEvent()
                val eventOrders = orderAdaptor.findByEventId(event.id!!)
                val excelOrders = getExcelOrders(eventOrders)
                s3PrivateFileUploadService.eventOrdersExcelUpload(
                    event.id!!, excelOrderHelper.execute(excelOrders)
                )
                RepeatStatus.FINISHED
            }
            .build()

    // 주문 상태가 결제 완료, 승인 완료 , 환불 , 취소 인것만 가져오도록 필터링.
    private fun getExcelOrders(eventOrders: List<Order>) =
        eventOrders
            .filter { it.orderStatus.isInEventOrderExcelStatus() }
            .map { ExcelOrderDto.from(it) }
}
