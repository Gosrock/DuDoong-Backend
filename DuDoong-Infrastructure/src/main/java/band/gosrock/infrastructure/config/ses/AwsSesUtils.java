package band.gosrock.infrastructure.config.ses;


import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailRequest.Builder;

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
}
