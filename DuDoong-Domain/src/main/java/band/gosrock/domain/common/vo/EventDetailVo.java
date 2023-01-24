package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDetailVo {
    // 포스터 이미지
    private String posterImage;

    // todo :: 디테일 이미지를 EventDetailImage 로 받기

    // 디테일 이미지 1
    private String detailImage1;

    // 디테일 이미지 2
    private String detailImage2;

    // 디테일 이미지 3
    private String detailImage3;

    // (마크다운) 공연 상세 내용
    private String content;

    public static EventDetailVo from(Event event) {
        EventDetail eventDetail = event.getEventDetail();
        if (eventDetail == null) {
            return null;
        }
        return EventDetailVo.builder()
                .posterImage(eventDetail.getPosterImage())
                .detailImage1(eventDetail.getDetailImage1())
                .detailImage2(eventDetail.getDetailImage2())
                .detailImage3(eventDetail.getDetailImage3())
                .content(eventDetail.getContent())
                .build();
    }
}
