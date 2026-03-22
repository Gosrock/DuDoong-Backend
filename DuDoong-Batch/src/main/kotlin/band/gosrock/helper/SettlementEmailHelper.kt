package band.gosrock.helper

import band.gosrock.common.annotation.Helper
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.infrastructure.config.s3.S3PrivateFileService
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import band.gosrock.infrastructure.config.ses.RawEmailAttachmentDto
import band.gosrock.infrastructure.config.ses.SendRawEmailDto
import jakarta.mail.MessagingException
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Helper
class SettlementEmailHelper(
    private val templateEngine: SpringTemplateEngine,
    private val s3PrivateFileService: S3PrivateFileService,
    private val awsSesUtils: AwsSesUtils,
) {

    private fun getSettlementPdfAttachment(event: Event): RawEmailAttachmentDto =
        RawEmailAttachmentDto(
            fileName = event.getEventName() + "_정산서.pdf",
            fileBytes = s3PrivateFileService.downloadEventSettlementPdf(event.id!!),
            type = "application/pdf",
        )

    private fun getOrderListExcelAttachment(event: Event): RawEmailAttachmentDto =
        RawEmailAttachmentDto(
            fileName = event.getEventName() + "_주문목록.xlsx",
            fileBytes = s3PrivateFileService.downloadEventOrdersExcel(event.id!!),
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )

    @Throws(MessagingException::class)
    fun sendToAdmin(event: Event) {
        val sendRawEmailDto = SendRawEmailDto(
            bodyHtml = templateEngine.process("eventSettlement", Context()),
            recipient = "support@dudoong.com",
            subject = event.getEventName() + "공연 정산서 어드민 발송 ( 관리자용 )",
        )

        sendRawEmailDto.addEmailAttachments(getSettlementPdfAttachment(event))
        sendRawEmailDto.addEmailAttachments(getOrderListExcelAttachment(event))
        awsSesUtils.sendRawEmails(sendRawEmailDto)
    }

    @Throws(MessagingException::class)
    fun sendToHost(event: Event, hostUserEmail: String) {
        val sendRawEmailDto = SendRawEmailDto(
            bodyHtml = templateEngine.process("eventSettlement", Context()),
            recipient = hostUserEmail,
            subject = event.getEventName() + "공연 정산관련 안내",
        )

        sendRawEmailDto.addEmailAttachments(getSettlementPdfAttachment(event))
        sendRawEmailDto.addEmailAttachments(getOrderListExcelAttachment(event))
        awsSesUtils.sendRawEmails(sendRawEmailDto)
    }
}
