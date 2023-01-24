package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventPlace;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventPlaceVo {
    // (지도 정보) 위도 - x
    private Double latitude;

    // (지도 정보) 경도 - y
    private Double longitude;

    // 공연 장소
    private String placeName;

    // 공연 상세 주소
    private String placeAddress;

    public static EventPlaceVo from(Event event) {
        EventPlace eventPlace = event.getEventPlace();
        if (eventPlace == null) {
            return EventPlaceVo.builder().build();
        }
        return EventPlaceVo.builder()
                .latitude(eventPlace.getLatitude())
                .longitude(eventPlace.getLongitude())
                .placeName(eventPlace.getPlaceName())
                .placeAddress(eventPlace.getPlaceAddress())
                .build();
    }
}
