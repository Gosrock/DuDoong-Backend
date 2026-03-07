package band.gosrock.infrastructure.config.ses

import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Properties
import javax.activation.DataHandler
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMessage.RecipientType
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.RawMessage
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest

@Component
class AwsSesUtils(
    private val sesClient: SesClient,
    private val templateEngine: SpringTemplateEngine,
) {
    private val log = LoggerFactory.getLogger(AwsSesUtils::class.java)

    fun singleEmailRequest(emailUserInfo: EmailUserInfo, subject: String, template: String, context: Context) {
        if (!emailUserInfo.receiveAgree) return
        val html = templateEngine.process(template, context)
        val sendEmailRequestBuilder = SendEmailRequest.builder()
        sendEmailRequestBuilder.destination(Destination.builder().toAddresses(emailUserInfo.email).build())
        sendEmailRequestBuilder
            .message(newMessage(subject, html))
            .source("life@dudoong.com")
            .build()
        sesClient.sendEmail(sendEmailRequestBuilder.build())
    }

    private fun newMessage(subject: String, html: String): Message {
        val content = Content.builder().data(subject).build()
        return Message.builder()
            .subject(content)
            .body(Body.builder().html { it.data(html) }.build())
            .build()
    }

    @Throws(MessagingException::class)
    fun sendRawEmails(sendRawEmailDto: SendRawEmailDto) {
        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        setRawEmailBaseInfo(sendRawEmailDto, message)
        val msg = MimeMultipart("mixed")
        message.setContent(msg)
        setBodyHtml(sendRawEmailDto, msg)
        setAttachments(sendRawEmailDto, msg)
        try {
            sesClient.sendRawEmail(buildSendRawEmailRequest(message))
        } catch (ex: Exception) {
            log.info(ex.toString())
            ex.printStackTrace()
        }
    }

    private fun buildSendRawEmailRequest(message: MimeMessage): SendRawEmailRequest {
        val outputStream = ByteArrayOutputStream()
        message.writeTo(outputStream)
        val rawMessage =
            RawMessage.builder()
                .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray())))
                .build()
        return SendRawEmailRequest.builder().rawMessage(rawMessage).build()
    }

    private fun setAttachments(sendRawEmailDto: SendRawEmailDto, msg: MimeMultipart) {
        sendRawEmailDto.rawEmailAttachments.forEach { setAttachmentToMessage(msg, it) }
    }

    @Throws(MessagingException::class)
    private fun setBodyHtml(sendRawEmailDto: SendRawEmailDto, msg: MimeMultipart) {
        val htmlPart = MimeBodyPart()
        htmlPart.setContent(sendRawEmailDto.bodyHtml, "text/html; charset=UTF-8")
        msg.addBodyPart(htmlPart)
    }

    private fun setAttachmentToMessage(msg: MimeMultipart, rawEmailAttachmentDto: RawEmailAttachmentDto) {
        try {
            val att = MimeBodyPart()
            val fds = ByteArrayDataSource(rawEmailAttachmentDto.fileBytes, rawEmailAttachmentDto.type)
            att.dataHandler = DataHandler(fds)
            att.fileName = rawEmailAttachmentDto.fileName
            msg.addBodyPart(att)
        } catch (e: Exception) {
            log.info(e.toString())
            e.printStackTrace()
        }
    }

    @Throws(MessagingException::class)
    private fun setRawEmailBaseInfo(sendRawEmailDto: SendRawEmailDto, message: MimeMessage) {
        message.setSubject(sendRawEmailDto.subject, "UTF-8")
        message.setFrom(InternetAddress(sendRawEmailDto.sender))
        message.setRecipients(RecipientType.TO, InternetAddress.parse(sendRawEmailDto.recipient))
    }
}
