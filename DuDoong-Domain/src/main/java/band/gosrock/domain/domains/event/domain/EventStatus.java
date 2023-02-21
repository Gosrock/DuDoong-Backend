package band.gosrock.domain.domains.event.domain;


import band.gosrock.common.annotation.EnumClass;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EnumClass
public enum EventStatus {
    PREPARING("PREPARING", "준비중"),
    OPEN("OPEN", "진행중"),
    CALCULATING("CALCULATING", "정산중"),
    CLOSED("CLOSED", "지난공연"),
    DELETED("DELETED", "삭제된공연");

    private final String name;
    @JsonValue private final String value;
}
