package band.gosrock.domain.domains.event.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.EventInfoVo;
import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.event.exception.EventCannotEndBeforeStartException;
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

    // 공연 시작 시각
    private LocalDateTime startAt;

    // 공연 종료 시각
    private LocalDateTime endAt;

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
    private String urlName;

    // 공연 장소
    private String placeName;

    // 공연 상세 주소
    private String placeAddress;

    // 이벤트 상태
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.PREPARING;

    // (마크다운) 공연 상세 내용
    private String content;

    // 예매 시작 시각
    private LocalDateTime ticketingStartAt;

    // 예매 종료 시각
    private LocalDateTime ticketingEndAt;

    /** 이벤트의 시작과 종료 시간을 지정 */
    public void setTime(LocalDateTime startAt, LocalDateTime endAt) {
        // 이벤트 종료가 시작보다 빠르면 안됨
        if (startAt.isAfter(endAt)) {
            throw EventCannotEndBeforeStartException.EXCEPTION;
        }
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /** 티켓팅 시작과 종료 시간을 지정 */
    public void setTicketingTime(LocalDateTime startAt, LocalDateTime endAt) {
        // 이벤트 종료가 시작보다 빠르면 안됨
        if (startAt.isAfter(endAt)) {
            throw EventCannotEndBeforeStartException.EXCEPTION;
        }
        this.ticketingStartAt = startAt;
        this.ticketingEndAt = endAt;
    }

    public void setUrlName(String urlName) {
        this.urlName = urlName;
    }

    @Builder
    public Event(
            Long hostId,
            String name,
            Long runTime,
            Double latitude,
            Double longitude,
            String posterImage,
            String urlName,
            String placeName,
            String placeAddress,
            String content) {
        this.hostId = hostId;
        this.runTime = runTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.posterImage = posterImage;
        this.name = name;
        this.urlName = urlName;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.content = content;
    }

    public RefundInfoVo getRefundInfoVo() {
        return RefundInfoVo.from(startAt);
    }

    public EventInfoVo toEventInfoVo() {
        return EventInfoVo.from(this);
    }
}
