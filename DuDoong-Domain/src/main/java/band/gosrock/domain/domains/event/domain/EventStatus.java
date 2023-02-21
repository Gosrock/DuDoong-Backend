package band.gosrock.domain.domains.event.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
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
    CLOSED("CLOSED", "지난공연"),

    // 삭제된 공연
    DELETED("DELETED", "삭제된공연");

    private final String name;
    @JsonValue private final String value;

    // Enum Validation 을 위한 코드, enum 에 속하지 않으면 null 리턴
    @JsonCreator
    public static EventStatus fromEventStatus(String val) {
        return Arrays.stream(values())
                .filter(type -> type.getName().equals(val))
                .findAny()
                .orElse(null);
    }
}
