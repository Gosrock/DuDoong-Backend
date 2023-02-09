package band.gosrock.domain.common.alarm.host;


import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostSlackAlarm {

    public static String joinOf(Host host, User user) {
        return user.toUserProfileVo().getUserName()
                + "님이 "
                + host.toHostProfileVo().getName()
                + "에 가입했습니다!";
    }

    public static String slackRegistrationOf(Host host) {
        return host.toHostProfileVo().getName() + "에 슬랙 알림이 등록되었습니다!";
    }
}
