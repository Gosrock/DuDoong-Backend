package band.gosrock.api.email.service;


import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class SendRegisterEmailService {
    private final AwsSesUtils awsSesUtils;

    public void execute(EmailUserInfo emailUserInfo) {
        Context context = new Context();
        context.setVariable("username", emailUserInfo.getName());
        awsSesUtils.singleEmailRequest(emailUserInfo, "두둥에 회원가입하신것을 축하드립니다!", "signUp", context);
    }
}
