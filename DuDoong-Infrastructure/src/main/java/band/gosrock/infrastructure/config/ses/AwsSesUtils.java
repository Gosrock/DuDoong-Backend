package band.gosrock.infrastructure.config.ses;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailRequest.Builder;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@Component
@AllArgsConstructor
public class AwsSesUtils {

    private final SesAsyncClient sesAsyncClient;
    private final SpringTemplateEngine templateEngine;

    public void singleEmailRequest(String to, String subject, String template, Context context)
        throws ExecutionException, InterruptedException {
        String html = templateEngine.process(template, context);

        Builder sendEmailRequestBuilder = SendEmailRequest.builder();
        sendEmailRequestBuilder.destination(Destination.builder().toAddresses(to).build());
        sendEmailRequestBuilder.message(newMessage(subject, html)).source("life@dudoong.com").build();

        CompletableFuture<SendEmailResponse> sendEmailResponseCompletableFuture = sesAsyncClient.sendEmail(
            sendEmailRequestBuilder.build());
        SendEmailResponse sendEmailResponse = sendEmailResponseCompletableFuture.get();
    }

    private Message newMessage(String subject, String html) {
        Content content = Content.builder().data(subject).build();
        return Message.builder().subject(content).body(
            Body.builder().html(builder -> builder.data(html)).build()).build();
    }

}
