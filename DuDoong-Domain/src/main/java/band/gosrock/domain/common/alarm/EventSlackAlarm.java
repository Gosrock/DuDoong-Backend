package band.gosrock.domain.common.alarm;


import band.gosrock.domain.domains.event.domain.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventSlackAlarm {

    public static String creationOf(String eventName) {
        return "새로운 공연 '" + eventName + "'이(가) 열렸습니다!";
    }

    public static String changeStatusOf(Event event) {
        return nameOf(event) + "의 상태가 " + statusOf(event) + "으로 변경되었습니다.";
    }

    public static String changeContentOf(Event event) {
        return nameOf(event) + "의 내용이 변경되었습니다. 확인해주세요!";
    }

    public static String deletionOf(String eventName) {
        return "'" + eventName + "' 공연이 삭제되었습니다.";
    }

    private static String nameOf(Event event) {
        return "'" + event.getEventBasic().getName() + "'";
    }

    private static String statusOf(Event event) {
        return "'" + event.getStatus().getValue() + "'";
    }
}
