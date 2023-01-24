package band.gosrock.domain.domains.event.domain;


import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventDetail {
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

    @Builder
    public EventDetail(
            String posterImage,
            String detailImage1,
            String detailImage2,
            String detailImage3,
            String content) {
        this.posterImage = posterImage;
        this.detailImage1 = detailImage1;
        this.detailImage2 = detailImage2;
        this.detailImage3 = detailImage3;
        this.content = content;
    }
}
