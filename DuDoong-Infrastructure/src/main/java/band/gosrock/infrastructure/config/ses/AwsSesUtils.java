package band.gosrock.infrastructure.config.ses;


import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
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


        // Replace sender@example.com with your "From" address.
        // This address must be verified with Amazon SES.
        private static String SENDER = "Sender Name <support@dudoong.com>";

        // Replace recipient@example.com with a "To" address. If your account
        // is still in the sandbox, this address must be verified.
        private static String RECIPIENT = "water0641@naver.com";

        // Specify a configuration set. If you do not want to use a configuration
        // set, comment the following variable, and the
        // ConfigurationSetName=CONFIGURATION_SET argument below.
        private static String CONFIGURATION_SET = "ConfigSet";

        // The subject line for the email.
        private static String SUBJECT = "Customer service contact info";

        // The email body for recipients with non-HTML email clients.
        private static String BODY_TEXT = "Hello,\r\n"
            + "Please see the attached file for a list "
            + "of customers to contact.";

        // The HTML body of the email.
        private static String BODY_HTML = "<html>"
            + "<head></head>"
            + "<body>"
            + "<h1>Hello!</h1>"
            + "<p>Please see the attached file for a "
            + "list of customers to contact.</p>"
            + "</body>"
            + "</html>";

        public void sendRawEmails(byte[] eventSettlementPdf) throws AddressException, MessagingException, IOException {

            Session session = Session.getDefaultInstance(new Properties());

            // Create a new MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Add subject, from and to lines.
            message.setSubject(SUBJECT, "UTF-8");
            message.setFrom(new InternetAddress(SENDER));

            message.setRecipients(RecipientType.TO, InternetAddress.parse(RECIPIENT));

            // Create a multipart/alternative child container.
            MimeMultipart msg_body = new MimeMultipart("alternative");

            // Create a wrapper for the HTML and text parts.
            MimeBodyPart wrap = new MimeBodyPart();

            // Define the text part.
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(BODY_TEXT, "text/plain; charset=UTF-8");

            // Define the HTML part.
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(BODY_HTML,"text/html; charset=UTF-8");

            // Add the text and HTML parts to the child container.
            msg_body.addBodyPart(textPart);
            msg_body.addBodyPart(htmlPart);

            // Add the child container to the wrapper object.
            wrap.setContent(msg_body);

            // Create a multipart/mixed parent container.
            MimeMultipart msg = new MimeMultipart("mixed");

            // Add the parent container to the message.
            message.setContent(msg);

            // Add the multipart/alternative part to the message.
            msg.addBodyPart(wrap);

            // Define the attachment
            MimeBodyPart att = new MimeBodyPart();
            DataSource fds = new ByteArrayDataSource(eventSettlementPdf,"application/pdf");
            att.setDataHandler(new DataHandler(fds));
            att.setFileName("이벤트 정산서.pdf");

            // Add the attachment to the message.
            msg.addBodyPart(att);

            // Try to send the email.
            try {
                System.out.println("Attempting to send an email through Amazon SES "
                    +"using the AWS SDK for Java...");

                // Print the raw email content on the console
                PrintStream out = System.out;
                message.writeTo(out);

                // Send the email.
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                message.writeTo(outputStream);
                RawMessage rawMessage =
                    RawMessage.builder().data(
                        SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray()))).build();
                SendRawEmailRequest rawEmailRequest =
                    SendRawEmailRequest.builder().rawMessage(rawMessage)
//                        .configurationSetName(CONFIGURATION_SET)
                        .build();

                sesClient.sendRawEmail(rawEmailRequest);
                System.out.println("Email sent!");
                // Display an error if something goes wrong.
            } catch (Exception ex) {
                System.out.println("Email Failed");
                System.err.println("Error message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

}
