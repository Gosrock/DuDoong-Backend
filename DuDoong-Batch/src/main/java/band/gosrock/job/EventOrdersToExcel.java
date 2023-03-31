package band.gosrock.job;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.excel.ExcelOrderDto;
import band.gosrock.excel.ExcelOrderHelper;
import band.gosrock.infrastructure.config.s3.S3PrivateFileService;
import band.gosrock.parameter.EventJobParameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공연 관련 주문 목록을 엑셀화 하여 프라이빗한 S3에 저장합니다. */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class EventOrdersToExcel {

    private static final String JOB_NAME = "이벤트주문목록_엑셀업로드";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final ExcelOrderHelper excelOrderHelper;
    private final EventAdaptor eventAdaptor;
    private final OrderAdaptor orderAdaptor;

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

    private final S3PrivateFileService s3PrivateFileUploadService;

    @Bean(BEAN_PREFIX + "eventJobParameter")
    @JobScope
    public EventJobParameter eventJobParameter() {
        return new EventJobParameter(eventAdaptor);
    }

    @Bean(JOB_NAME)
    public Job slackUserStatisticJob() {
        return jobBuilderFactory.get(JOB_NAME).preventRestart().start(userStatisticStep()).build();
    }

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    public Step userStatisticStep() {
        return stepBuilderFactory
                .get(BEAN_PREFIX + "step")
                .tasklet(
                        (contribution, chunkContext) -> {
                            Event event = eventJobParameter.getEvent();
                            List<Order> eventOrders = orderAdaptor.findByEventId(event.getId());

                            List<ExcelOrderDto> excelOrders = getExcelOrders(eventOrders);
                            s3PrivateFileUploadService.eventOrdersExcelUpload(
                                    event.getId(), excelOrderHelper.execute(excelOrders));

                            return RepeatStatus.FINISHED;
                        })
                .build();
    }

    // 주문 상태가 결제 완료, 승인 완료 , 환불 , 취소 인것만 가져오도록 필터링.
    private List<ExcelOrderDto> getExcelOrders(List<Order> eventOrders) {
        return eventOrders.stream()
                .filter(order -> order.getOrderStatus().isInEventOrderExcelStatus())
                .map(ExcelOrderDto::from)
                .toList();
    }
}
