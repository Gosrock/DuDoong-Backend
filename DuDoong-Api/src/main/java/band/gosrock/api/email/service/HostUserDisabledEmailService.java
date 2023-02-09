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
public class HostUserDisabledEmailService {
    private final AwsSesUtils awsSesUtils;

    public void execute(EmailUserInfo userInfo, String hostName, HostRole hostRole) {
        Context context = new Context();
        context.setVariable("userInfo", userInfo);
        context.setVariable("hostName", hostName);
        context.setVariable("role", hostRole.getValue());
        log.info(hostName + " 에서 추방당함, " + userInfo); //
        // todo : 호스트에서 추방 템플릿 추가
        //        awsSesUtils.singleEmailRequest(
        //                userInfo.getEmail(), "두둥" + hostName + " 호스트 초대 알림 드립니다.", "hostInvite",
        // context);
    }
}
