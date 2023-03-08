package band.gosrock.domain.common.alarm;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettlementKakaoTalkAlarm {

    public static String creationOf(String hostName) {
        return "안녕하세요 " + hostName + "님!\n" + "이메일로 정산서 발송이 완료되어 안내드립니다.";
    }

    public static String creationHeaderOf() {
        return "정산서 이메일 발송 안내";
    }

    public static String creationTemplateCode() {
        return "settlement";
    }
}
