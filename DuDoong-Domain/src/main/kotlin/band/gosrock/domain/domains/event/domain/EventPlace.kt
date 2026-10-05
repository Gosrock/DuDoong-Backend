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
    // v2 상세주소 (예: "지하 1층"). v2 기본 정보(E-4)에서만 입력, 없으면 null (#740, V010)
    var placeDetailAddress: String? = null,
) {
    /**
     * v1·운영 어드민 장소 수정은 상세주소를 모르므로, 주소가 그대로면 이전 상세주소를 유지하고 주소가 바뀌면 지운다
     * (다른 장소에 옛 상세주소가 남지 않게, #740)
     */
    fun keepingDetailOf(previous: EventPlace?): EventPlace {
        if (placeDetailAddress == null && previous != null && previous.placeAddress == placeAddress) {
            placeDetailAddress = previous.placeDetailAddress
        }
        return this
    }

    fun isUpdated(): Boolean =
        this.latitude != null && this.longitude != null && this.placeName != null && this.placeAddress != null
}
