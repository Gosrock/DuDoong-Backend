package band.gosrock.domain.domains.event.domain

import band.gosrock.domain.common.vo.ImageVo
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded

@Embeddable
class EventDetail() {

    // 포스터 이미지
    @Embedded
    var posterImage: ImageVo? = null
        protected set

    // (마크다운) 공연 상세 내용
    @Column(columnDefinition = "TEXT")
    var content: String? = null
        protected set

    constructor(posterImageKey: String?, content: String?) : this() {
        this.posterImage = posterImageKey?.let { ImageVo.valueOf(it) }
        this.content = content
    }

    fun isUpdated(): Boolean = this.posterImage != null && this.content != null

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var posterImageKey: String? = null
        private var content: String? = null

        fun posterImageKey(key: String?) = apply { this.posterImageKey = key }
        fun content(content: String?) = apply { this.content = content }
        fun build() = EventDetail(posterImageKey, content)
    }
}
