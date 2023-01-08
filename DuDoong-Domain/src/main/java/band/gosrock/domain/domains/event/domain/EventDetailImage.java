package band.gosrock.domain.domains.event.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_event_detail_image")
public class EventDetailImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_detail_image_id")
    private Long id;

    // 이벤트 정보
    private Long eventId;

    // 이미지 주소
    @Column(length = 15)
    private String imageUrl;

    @Builder
    public EventDetailImage(Long eventId, String imageUrl) {
        this.eventId = eventId;
        this.imageUrl = imageUrl;
    }
}
