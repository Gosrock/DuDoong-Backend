package band.gosrock.domain.domains.event.domain

import jakarta.persistence.Embeddable

@Embeddable
class EventPlace(
    // (지도 정보) 위도 - x
    var latitude: Double? = null,
    // (지도 정보) 경도 - y
    var longitude: Double? = null,
    // 공연 장소
    var placeName: String? = null,
    // 공연 상세 주소
    var placeAddress: String? = null,
) {
    fun isUpdated(): Boolean =
        this.latitude != null && this.longitude != null && this.placeName != null && this.placeAddress != null
}
