package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.event.domain.Event

data class EventDetailVo(
    // 포스터 이미지
    val posterImage: ImageVo? = null,
    // (마크다운) 공연 상세 내용
    val content: String? = null,
) {
    companion object {
        @JvmStatic
        fun from(event: Event): EventDetailVo {
            val eventDetail = event.eventDetail ?: return EventDetailVo()
            return EventDetailVo(
                posterImage = eventDetail.posterImage,
                content = eventDetail.content,
            )
        }
    }
}
