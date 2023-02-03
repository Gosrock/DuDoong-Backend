package band.gosrock.domain.domains.event.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventStatus {
    // 준비중
    PREPARING("PREPARING", "준비중"),

    // 진행중
    OPEN("OPEN", "진행중"),

    // 정산중
    CALCULATING("CALCULATING", "정산중"),

    // 지난 공연
    CLOSED("CLOSED", "지난공연");

    private final String name;
    @JsonValue private final String value;

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
