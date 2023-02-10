package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDetailVo {
    // 포스터 이미지
    private ImageVo posterImage;

    // (마크다운) 공연 상세 내용
    private String content;

    public static EventDetailVo from(Event event) {
        EventDetail eventDetail = event.getEventDetail();
        if (eventDetail == null) {
            return EventDetailVo.builder().build();
        }
        return EventDetailVo.builder()
                .posterImage(eventDetail.getPosterImage())
                .content(eventDetail.getContent())
                .build();
    }
}
