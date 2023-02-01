package band.gosrock.domain.domains.event.domain;


import band.gosrock.domain.common.vo.ImageVo;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventDetail {
    // 포스터 이미지
    @Embedded private ImageVo posterImage;

    // todo :: 디테일 이미지를 EventDetailImage 로 받기

    // 디테일 이미지
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> detailImages = new ArrayList<>();

    // (마크다운) 공연 상세 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    protected Boolean isUpdated() {
        return this.posterImage != null && this.content != null && !this.detailImages.isEmpty();
    }

    @Builder
    public EventDetail(String posterImageKey, List<String> detailImages, String content) {
        this.posterImage = ImageVo.valueOf(posterImageKey);
        this.detailImages = detailImages;
        this.content = content;
    }
}
