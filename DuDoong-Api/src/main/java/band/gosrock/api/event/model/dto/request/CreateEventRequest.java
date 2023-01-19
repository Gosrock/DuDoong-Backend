package band.gosrock.api.event.model.dto.request;


import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateEventRequest {
    // 공연 이름
    private String name;

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

    // url 표시 이름 (unique)
    private String url;

    // 공연 장소
    private String placeName;

    // 공연 상세 주소
    private String placeAddress;

    // (마크다운) 공연 상세 내용
    private String content;

    // 예매 시작 시각
    private LocalDateTime ticketingStartAt;

    // 예매 종료 시각
    private LocalDateTime ticketingEndAt;
}
