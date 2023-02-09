package band.gosrock.api.email.service;


import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo;
import band.gosrock.infrastructure.config.ses.AwsSesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostUserRoleChangeEmailService {
    private final AwsSesUtils awsSesUtils;

    public void execute(EmailUserInfo userInfo, String hostName, HostRole hostRole) {
        Context context = new Context();
        context.setVariable("userInfo", userInfo);
        context.setVariable("hostName", hostName);
        context.setVariable("role", hostRole.getValue());
        log.info(hostName + " 의 역할 변경 알림. " + userInfo); //
        // todo : 당신의 역할이 변경되었음을 알리는 템플릿 추가
    }
}
