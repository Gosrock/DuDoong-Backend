package band.gosrock.domain.domains.event.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_event")
public class Event extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    // 호스트 정보
    private Long hostId;

    // 공연 시작일
    private LocalDateTime startAt;

    // 공연 진행 시간
    private Long runTime;

    // (지도 정보) 위도 - x
    private Double latitude;

    // (지도 정보) 경도 - y
    private Double longitude;

    // 포스터 이미지
    private String posterImage;

    // 공연 이름
    private String name;

    // url 표시 이름 (unique)
    private String url;

    // 공연 장소
    private String placeName;

    // 공연 상세 주소
    private String placeAddress;

    // 이벤트 상태
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    // (마크다운) 공연 상세 내용
    private String content;

    // 예매 시작 시각
    private LocalDateTime ticketingStartAt;

    // 예매 종료 시각
    private LocalDateTime ticketingEndAt;

    @Builder
    public Event(
            Long hostId,
            LocalDateTime startAt,
            Long runTime,
            Double latitude,
            Double longitude,
            String posterImage,
            String name,
            String url,
            String placeName,
            String placeAddress,
            EventStatus status,
            String content,
            LocalDateTime ticketingStartAt,
            LocalDateTime ticketingEndAt) {
        this.hostId = hostId;
        this.startAt = startAt;
        this.runTime = runTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.posterImage = posterImage;
        this.name = name;
        this.url = url;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.status = status;
        this.content = content;
        this.ticketingStartAt = ticketingStartAt;
        this.ticketingEndAt = ticketingEndAt;
    }

    public RefundInfoVo getRefundInfoVo() {
        return RefundInfoVo.from(startAt);
    }

    public EventInfoVo toEventInfoVo() {
        return EventInfoVo.from(this);
    }
}
