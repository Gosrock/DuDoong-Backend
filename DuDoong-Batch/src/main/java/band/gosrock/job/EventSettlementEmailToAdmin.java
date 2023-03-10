package band.gosrock.job;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.s3.S3PrivateFileService;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import band.gosrock.infrastructure.config.ses.RawEmailAttachmentDto;
import band.gosrock.infrastructure.config.ses.SendRawEmailDto;
import band.gosrock.parameter.EventJobParameter;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class EventSettlementEmailToAdmin {

    private static final String JOB_NAME = "이벤트정산_이메일발송_어드민";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EventAdaptor eventAdaptor;

    private final HostAdaptor hostAdaptor;
    private final UserAdaptor userAdaptor;

    private final SpringTemplateEngine templateEngine;

    private final S3PrivateFileService s3PrivateFileUploadService;

    private final AwsSesUtils awsSesUtils;

    @Bean(BEAN_PREFIX + "eventJobParameter")
    @JobScope
    public EventJobParameter eventJobParameter() {
        return new EventJobParameter(eventAdaptor);
    }

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

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
                            String eventName = event.getEventBasic().getName();
                            Long eventId = event.getId();
                            Host host = hostAdaptor.findById(event.getHostId());
                            User masterUser = userAdaptor.queryUser(host.getMasterUserId());
                            byte[] eventSettlementPdf =
                                    s3PrivateFileUploadService.downloadEventSettlementPdf(eventId);
                            RawEmailAttachmentDto eventSettlementPdfAttachment =
                                    RawEmailAttachmentDto.builder()
                                            .fileName(eventName + "_정산서.pdf")
                                            .fileBytes(eventSettlementPdf)
                                            .type("application/pdf")
                                            .build();

                            byte[] eventOrderListExcel =
                                    s3PrivateFileUploadService.downloadEventOrdersExcel(eventId);
                            RawEmailAttachmentDto eventOrderListExcelAttachment =
                                    RawEmailAttachmentDto.builder()
                                            .fileName(eventName + "_주문목록.xlsx")
                                            .fileBytes(eventOrderListExcel)
                                            .type(
                                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                            .build();

                            SendRawEmailDto sendRawEmailDto =
                                    SendRawEmailDto.builder()
                                            .bodyHtml(
                                                    templateEngine.process(
                                                            "eventSettlement", new Context()))
                                            .recipient("support@dudoong.com")
                                            .subject(eventName + "공연 정산서 어드민 발송 ( 관리자용 )")
                                            .build();

                            sendRawEmailDto.addEmailAttachments(eventSettlementPdfAttachment);
                            sendRawEmailDto.addEmailAttachments(eventOrderListExcelAttachment);

                            awsSesUtils.sendRawEmails(sendRawEmailDto);
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
