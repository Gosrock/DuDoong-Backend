package band.gosrock.api.email.service;

import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class SendRegisterEmailService {
    private final AwsSesUtils awsSesUtils;

    public void execute(){
        Context context = new Context();
        context.setVariable("test", "testest");
        awsSesUtils.singleEmailRequest("water0641@naver.com","zzz", "signUp",context);
    }
}
