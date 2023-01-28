package band.gosrock.api.event.model.dto.response;


import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.domains.event.domain.Event;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

/** 이벤트 프로필만 표시하는 응답 DTO */
@Getter
@Builder
public class EventProfileResponse {
    @JsonUnwrapped private EventProfileVo eventProfileVo;

    public static EventProfileResponse of(Event event) {
        return EventProfileResponse.builder().eventProfileVo(event.toEventProfileVo()).build();
    }
}
