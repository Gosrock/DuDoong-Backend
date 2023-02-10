package band.gosrock.api.email.service;


import band.gosrock.api.email.dto.IssuedTicketMailDTO;
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EntranceIssuedTicketEmailService {

    private final AwsSesUtils awsSesUtils;

    public void execute(IssuedTicketMailDTO issuedTicketMailDTO) {
        String subject = "[두둥] 입장 확인 알림드립니다.";
        Context context = new Context();
        EmailUserInfo userInfo = issuedTicketMailDTO.getUserInfo();

        context.setVariable("userInfo", userInfo);
        context.setVariable("issuedTicketInfo", issuedTicketMailDTO.getIssuedTicketInfo());
        context.setVariable("eventInfo", issuedTicketMailDTO.getEventInfo());

        awsSesUtils.singleEmailRequest(
                userInfo.getEmail(), subject, "entranceIssuedTicket", context);
    }
}
