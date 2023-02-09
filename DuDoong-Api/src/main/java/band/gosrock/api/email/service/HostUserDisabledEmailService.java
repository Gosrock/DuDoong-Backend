package band.gosrock.api.email.service;


import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostUserDisabledEmailService {
    private final AwsSesUtils awsSesUtils;

    public void execute(EmailUserInfo userInfo, String hostName) {
        Context context = new Context();
        context.setVariable("userInfo", userInfo);
        context.setVariable("hostName", hostName);
        log.info(hostName + " 에서 추방당함, " + userInfo);
        // todo : 호스트에서 추방 템플릿 추가
    }
}
