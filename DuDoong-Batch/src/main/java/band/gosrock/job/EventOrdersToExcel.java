package band.gosrock.job;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.excel.ExcelOrderDto;
import band.gosrock.excel.ExcelOrderHelper;
import band.gosrock.infrastructure.config.s3.S3PrivateFileUploadService;
import band.gosrock.parameter.EventJobParameter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    private final S3PrivateFileUploadService s3PrivateFileUploadService;

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

                            // 주문 상태가 결제 완료, 승인 완료 , 환불 , 취소 인것만 가져오도록 필터링.
                            List<ExcelOrderDto> excelOrders =
                                    eventOrders.stream()
                                            .filter(
                                                    order ->
                                                            order.getOrderStatus()
                                                                    .isInEventOrderExcelStatus())
                                            .map(ExcelOrderDto::from)
                                            .toList();

                            // 엑셀에 만들기
                            // 주문 번호 , 주문 방식 , 유저아이디 , 주문 이름 , 매수 , 총 결제금액 , 일시 , 환불일시.
                            Workbook workbook = excelOrderHelper.execute(excelOrders);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            workbook.write(outputStream);
                            workbook.close();

                            String fileKey =
                                    s3PrivateFileUploadService.eventOrdersExcelUpload(
                                            event.getId(), outputStream);
                            // 이벤트 정산용 엑셀 파일 업로드 정보 저장
                            // 파일 키 형식 맞출꺼라서 필요없을듯.
                            //
                            // eventSettlementDomainService.updateEventOrderListExcelFileKey(
                            //                                    event.getId(), fileKey);

                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
