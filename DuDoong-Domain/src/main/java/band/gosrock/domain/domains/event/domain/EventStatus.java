package band.gosrock.domain.domains.event.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventStatus {
    // 준비중
    PREPARING("PREPARING"),
    // 진행중
    OPEN("OPEN"),
    // 종료
    CLOSED("CLOSED");

    private final String value;

    // Enum Validation 을 위한 코드, enum 에 속하지 않으면 null 리턴
    @JsonCreator
    public static EventStatus fromEventStatus(String val) {
        for (EventStatus eventStatus : EventStatus.values()) {
            if (eventStatus.name().equals(val)) {
                return eventStatus;
            }
        }
        return null;
    }
}
