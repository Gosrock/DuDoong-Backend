package band.gosrock.domain.domains.event.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "tbl_event_detail_image")
class EventDetailImage() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_detail_image_id")
    var id: Long? = null
        protected set

    // 이벤트 정보
    var eventId: Long? = null
        protected set

    // 이미지 주소
    var imageUrl: String? = null
        protected set

    constructor(eventId: Long?, imageUrl: String?) : this() {
        this.eventId = eventId
        this.imageUrl = imageUrl
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var eventId: Long? = null
        private var imageUrl: String? = null

        fun eventId(eventId: Long?) = apply { this.eventId = eventId }
        fun imageUrl(imageUrl: String?) = apply { this.imageUrl = imageUrl }
        fun build() = EventDetailImage(eventId, imageUrl)
    }
}
