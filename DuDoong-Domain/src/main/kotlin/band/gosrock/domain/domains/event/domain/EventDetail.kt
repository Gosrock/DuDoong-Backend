package band.gosrock.domain.domains.event.domain

import band.gosrock.domain.common.vo.ImageVo
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
class EventDetail(
    posterImageKey: String? = null,

    // (마크다운) 공연 상세 내용
    @Column(columnDefinition = "TEXT")
    var content: String? = null,
) {
    // 포스터 이미지
    @Embedded
    var posterImage: ImageVo? = posterImageKey?.let { ImageVo.valueOf(it) }

    fun isUpdated(): Boolean = this.posterImage != null && this.content != null
}
