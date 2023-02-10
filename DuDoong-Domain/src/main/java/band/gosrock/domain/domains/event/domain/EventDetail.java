package band.gosrock.domain.domains.event.domain;


import band.gosrock.domain.common.vo.ImageVo;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
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

    // (마크다운) 공연 상세 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    protected Boolean isUpdated() {
        return this.posterImage != null && this.content != null;
    }

    @Builder
    public EventDetail(String posterImageKey, String content) {
        this.posterImage = ImageVo.valueOf(posterImageKey);
        this.content = content;
    }
}
