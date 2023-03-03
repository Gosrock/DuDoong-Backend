package band.gosrock.infrastructure.config.ses;


import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SendRawEmailDto {
    // Replace sender@example.com with your "From" address.
    // This address must be verified with Amazon SES.
    private final String sender = "공연 정산관리팀 <support@dudoong.com>";
    // Replace recipient@example.com with a "To" address. If your account
    // is still in the sandbox, this address must be verified.
    private final String recipient;
    // The subject line for the email.
    private final String subject;

    private final String bodyHtml;

    private final List<RawEmailAttachmentDto> rawEmailAttachments = new ArrayList<>();

    @Builder
    public SendRawEmailDto(String recipient, String subject, String bodyHtml) {
        this.recipient = recipient;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
    }

    public void addEmailAttachments(RawEmailAttachmentDto rawEmailAttachment) {
        rawEmailAttachments.add(rawEmailAttachment);
    }
}
