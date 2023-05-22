package band.gosrock.helper;


import band.gosrock.common.annotation.Helper;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.infrastructure.config.s3.S3PrivateFileService;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import band.gosrock.infrastructure.config.ses.RawEmailAttachmentDto;
import band.gosrock.infrastructure.config.ses.SendRawEmailDto;
import javax.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Helper
@RequiredArgsConstructor
public class SettlementEmailHelper {
    private final SpringTemplateEngine templateEngine;

    private final S3PrivateFileService s3PrivateFileService;
    private final AwsSesUtils awsSesUtils;

    private RawEmailAttachmentDto getSettlementPdfAttachment(Event event) {
        return RawEmailAttachmentDto.builder()
                .fileName(event.getEventName() + "_정산서.pdf")
                .fileBytes(s3PrivateFileService.downloadEventSettlementPdf(event.getId()))
                .type("application/pdf")
                .build();
    }

    private RawEmailAttachmentDto getOrderListExcelAttachment(Event event) {
        return RawEmailAttachmentDto.builder()
                .fileName(event.getEventName() + "_주문목록.xlsx")
                .fileBytes(s3PrivateFileService.downloadEventOrdersExcel(event.getId()))
                .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .build();
    }

    public void sendToAdmin(Event event) throws MessagingException {
        SendRawEmailDto sendRawEmailDto =
                SendRawEmailDto.builder()
                        .bodyHtml(templateEngine.process("eventSettlement", new Context()))
                        .recipient("support@dudoong.com")
                        .subject(event.getEventName() + "공연 정산서 어드민 발송 ( 관리자용 )")
                        .build();

        sendRawEmailDto.addEmailAttachments(getSettlementPdfAttachment(event));
        sendRawEmailDto.addEmailAttachments(getOrderListExcelAttachment(event));
        awsSesUtils.sendRawEmails(sendRawEmailDto);
    }

    public void sendToHost(Event event, String hostUserEmail) throws MessagingException {
        SendRawEmailDto sendRawEmailDto =
                SendRawEmailDto.builder()
                        .bodyHtml(templateEngine.process("eventSettlement", new Context()))
                        .recipient(hostUserEmail)
                        .subject(event.getEventName() + "공연 정산관련 안내")
                        .build();

        sendRawEmailDto.addEmailAttachments(getSettlementPdfAttachment(event));
        sendRawEmailDto.addEmailAttachments(getOrderListExcelAttachment(event));
        awsSesUtils.sendRawEmails(sendRawEmailDto);
    }
}
