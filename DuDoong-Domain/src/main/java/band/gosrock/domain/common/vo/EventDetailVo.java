package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventDetail;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDetailVo {
    // 포스터 이미지
    private String posterImage;

    // todo :: 디테일 이미지를 EventDetailImage 로 받기

    // 디테일 이미지
    private List<String> detailImages;

    // (마크다운) 공연 상세 내용
    private String content;

    public static EventDetailVo from(Event event) {
        EventDetail eventDetail = event.getEventDetail();
        if (eventDetail == null) {
            return EventDetailVo.builder().build();
        }
        return EventDetailVo.builder()
                .posterImage(eventDetail.getPosterImage())
                .detailImages(eventDetail.getDetailImages())
                .content(eventDetail.getContent())
                .build();
    }
}
