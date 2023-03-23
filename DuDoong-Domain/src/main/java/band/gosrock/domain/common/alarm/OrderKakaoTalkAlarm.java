package band.gosrock.domain.common.alarm;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderKakaoTalkAlarm {

    public static String creationOf(String userName, String hostName, String eventName) {
        return "안녕하세요 "
                + userName
                + "님!\n"
                + hostName
                + " "
                + eventName
                + "티켓이 발급됐습니다.\n"
                + "\n"
                + "원활한 입장을 위해 QR코드를 미리 준비해주세요.";
    }

    public static String creationHeaderOf() {
        return "주문 완료 안내";
    }

    public static String creationTemplateCode() {
        return "doneorderv2";
    }

    public static String deletionOf(String userName, String hostName, String eventName) {
        return "안녕하세요 " + userName + "님!\n" + hostName + " " + eventName + " 주문이 취소되어 안내드립니다.\n";
    }

    public static String deletionHeaderOf() {
        return "주문 취소 안내";
    }

    public static String deletionTemplateCode() {
        return "cancelorder";
    }
}
