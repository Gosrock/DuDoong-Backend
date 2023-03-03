package band.gosrock.domain.common.alarm;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserKakaoTalkAlarm {

    public static String creationOf(String userName) {
        return userName
                + "님, 두둥에 가입하신 것을 환영합니다!\n"
                + "\n"
                + "두둥은 누구나 자유롭게 공연을 홍보하고 인원을 모집할 수 있는 서비스입니다.\n"
                + "호스트가 되어 밴드, 뮤지컬, 버스킹등의 공연을 열고 홍보하세요!\n"
                + "\n";
    }

    public static String creationTemplateCode() {
        return "signup";
    }
}
