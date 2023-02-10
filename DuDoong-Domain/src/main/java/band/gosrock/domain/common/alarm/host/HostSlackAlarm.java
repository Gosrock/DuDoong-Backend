package band.gosrock.domain.common.alarm.host;


import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HostSlackAlarm {

    public static String joinOf(Host host, User user) {
        return nameOf(user) + "님이 " + nameOf(host) + "에 가입했습니다!";
    }

    public static String slackRegistrationOf(Host host) {
        return nameOf(host) + "에 슬랙 알림이 등록되었습니다! 환영합니다";
    }

    public static String changeMasterOf(Host host, User user) {
        return nameOf(host) + "의 마스터가 " + nameOf(user) + "님으로 변경되었습니다.";
    }

    public static String disabledOf(User user) {
        return nameOf(user) + "님이 호스트에서 추방당했습니다.";
    }

    private static String nameOf(Host host) {
        return "'" + host.getProfile().getName() + "'";
    }

    private static String nameOf(User user) {
        return "'" + user.getProfile().getName() + "'";
    }
}
