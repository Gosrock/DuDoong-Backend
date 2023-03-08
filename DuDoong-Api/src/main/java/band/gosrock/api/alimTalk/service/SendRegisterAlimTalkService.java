package band.gosrock.api.alimTalk.service;


import band.gosrock.domain.common.alarm.UserKakaoTalkAlarm;
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendRegisterAlimTalkService {
    private final NcpHelper ncpHelper;

    public void execute(String userName, String to) {
        String content = UserKakaoTalkAlarm.creationOf(userName);
        ncpHelper.sendButtonNcpAlimTalk(to, UserKakaoTalkAlarm.creationTemplateCode(), content);
    }
}
