package band.gosrock.domain.domains.event.domain

import javax.persistence.Embeddable

@Embeddable
class EventPlace() {

    // (지도 정보) 위도 - x
    var latitude: Double? = null
        protected set

    // (지도 정보) 경도 - y
    var longitude: Double? = null
        protected set

    // 공연 장소
    var placeName: String? = null
        protected set

    // 공연 상세 주소
    var placeAddress: String? = null
        protected set

    constructor(
        latitude: Double?,
        longitude: Double?,
        placeName: String?,
        placeAddress: String?,
    ) : this() {
        this.latitude = latitude
        this.longitude = longitude
        this.placeName = placeName
        this.placeAddress = placeAddress
    }

    fun isUpdated(): Boolean =
        this.latitude != null && this.longitude != null && this.placeName != null && this.placeAddress != null

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var latitude: Double? = null
        private var longitude: Double? = null
        private var placeName: String? = null
        private var placeAddress: String? = null

        fun latitude(latitude: Double?) = apply { this.latitude = latitude }
        fun longitude(longitude: Double?) = apply { this.longitude = longitude }
        fun placeName(placeName: String?) = apply { this.placeName = placeName }
        fun placeAddress(placeAddress: String?) = apply { this.placeAddress = placeAddress }
        fun build() = EventPlace(latitude, longitude, placeName, placeAddress)
    }
}
