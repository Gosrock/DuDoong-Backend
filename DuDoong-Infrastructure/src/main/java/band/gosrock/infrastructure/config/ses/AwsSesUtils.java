package band.gosrock.infrastructure.config.ses;


import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailRequest.Builder;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Component
@AllArgsConstructor
@Slf4j
public class AwsSesUtils {

    private final SesClient sesClient;
    private final SpringTemplateEngine templateEngine;

    public void singleEmailRequest(
            EmailUserInfo emailUserInfo, String subject, String template, Context context) {
        // 이메일 수신거부시 발송안함
        if (!emailUserInfo.getReceiveAgree()) {
            return;
        }
        String html = templateEngine.process(template, context);

        Builder sendEmailRequestBuilder = SendEmailRequest.builder();
        sendEmailRequestBuilder.destination(
                Destination.builder().toAddresses(emailUserInfo.getEmail()).build());
        sendEmailRequestBuilder
                .message(newMessage(subject, html))
                .source("life@dudoong.com")
                .build();

        sesClient.sendEmail(sendEmailRequestBuilder.build());
    }

    private Message newMessage(String subject, String html) {
        Content content = Content.builder().data(subject).build();
        return Message.builder()
                .subject(content)
                .body(Body.builder().html(builder -> builder.data(html)).build())
                .build();
    }
    // The HTML body of the email.

    public void sendRawEmails(SendRawEmailDto sendRawEmailDto)
            throws AddressException, MessagingException, IOException {

        Session session = Session.getDefaultInstance(new Properties());
        // Create a new MimeMessage object.
        MimeMessage message = new MimeMessage(session);
        // 제목 송신, 수신자 설정
        setRawEmailBaseInfo(sendRawEmailDto, message);
        // Create a multipart/mixed parent container.
        MimeMultipart msg = new MimeMultipart("mixed");
        // Add the parent container to the message.
        message.setContent(msg);
        // Define the HTML part.
        setBodyHtml(sendRawEmailDto, msg);

        setAttachments(sendRawEmailDto, msg);
        // Try to send the email.
        try {
            // Send the email.
            sesClient.sendRawEmail(buildSendRawEmailRequest(message));
        } catch (Exception ex) {
            log.info(ex.toString());
            ex.printStackTrace();
        }
    }

    private static SendRawEmailRequest buildSendRawEmailRequest(MimeMessage message)
            throws IOException, MessagingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        RawMessage rawMessage =
                RawMessage.builder()
                        .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray())))
                        .build();
        SendRawEmailRequest rawEmailRequest =
                SendRawEmailRequest.builder().rawMessage(rawMessage).build();
        return rawEmailRequest;
    }

    private void setAttachments(SendRawEmailDto sendRawEmailDto, MimeMultipart msg) {
        List<RawEmailAttachmentDto> rawEmailAttachments = sendRawEmailDto.getRawEmailAttachments();
        rawEmailAttachments.forEach(
                rawEmailAttachmentDto -> setAttachmentToMessage(msg, rawEmailAttachmentDto));
    }

    private void setBodyHtml(SendRawEmailDto sendRawEmailDto, MimeMultipart msg)
            throws MessagingException {
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(sendRawEmailDto.getBodyHtml(), "text/html; charset=UTF-8");
        // html 추가
        msg.addBodyPart(htmlPart);
    }

    private void setAttachmentToMessage(
            MimeMultipart msg, RawEmailAttachmentDto rawEmailAttachmentDto) {
        try {
            MimeBodyPart att = new MimeBodyPart();
            DataSource fds =
                    new ByteArrayDataSource(
                            rawEmailAttachmentDto.getFileBytes(), rawEmailAttachmentDto.getType());
            att.setDataHandler(new DataHandler(fds));
            att.setFileName(rawEmailAttachmentDto.getFileName());
            msg.addBodyPart(att);
        } catch (Exception e) {
            log.info(e.toString());
            e.printStackTrace();
        }
    }

    private void setRawEmailBaseInfo(SendRawEmailDto sendRawEmailDto, MimeMessage message)
            throws MessagingException {
        // Add subject, from and to lines.
        message.setSubject(sendRawEmailDto.getSubject(), "UTF-8");
        message.setFrom(new InternetAddress(sendRawEmailDto.getSender()));
        message.setRecipients(
                RecipientType.TO, InternetAddress.parse(sendRawEmailDto.getRecipient()));
    }
}
