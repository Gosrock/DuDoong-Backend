package band.gosrock.domain.domains.event.domain;


import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventPlace {

    // (지도 정보) 위도 - x
    private Double latitude;

    // (지도 정보) 경도 - y
    private Double longitude;

    // 공연 장소
    private String placeName;

    // 공연 상세 주소
    private String placeAddress;

    @Builder
    public EventPlace(Double latitude, Double longitude, String placeName, String placeAddress) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
    }
}
