package band.gosrock.domain.domains.event.domain;


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
}
