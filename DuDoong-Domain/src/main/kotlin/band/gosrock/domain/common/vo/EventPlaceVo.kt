package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.event.domain.Event

data class EventPlaceVo(
    // (지도 정보) 위도 - x
    val latitude: Double? = null,
    // (지도 정보) 경도 - y
    val longitude: Double? = null,
    // 공연 장소
    val placeName: String? = null,
    // 공연 상세 주소
    val placeAddress: String? = null,
) {
    companion object {
        @JvmStatic
        fun from(event: Event): EventPlaceVo {
            val eventPlace = event.eventPlace ?: return EventPlaceVo()
            return EventPlaceVo(
                latitude = eventPlace.latitude,
                longitude = eventPlace.longitude,
                placeName = eventPlace.placeName,
                placeAddress = eventPlace.placeAddress,
            )
        }
    }
}
