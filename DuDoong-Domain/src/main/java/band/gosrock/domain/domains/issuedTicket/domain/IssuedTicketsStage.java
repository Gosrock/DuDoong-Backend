package band.gosrock.domain.domains.issuedTicket.domain;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssuedTicketsStage {
    // 입장완료
    AFTER_ENTRANCE("AFTER_ENTRANCE", "입장완료"),
    // 관람예정
    BEFORE_ENTRANCE("BEFORE_ENTRANCE", "관람예정"),
    // 입장중
    ENTERING("ENTERING", "입장중"),
    // 취소됨
    CANCELED("CANCELED", "취소됨");
    private final String value;

    @JsonValue private final String kr;
}
