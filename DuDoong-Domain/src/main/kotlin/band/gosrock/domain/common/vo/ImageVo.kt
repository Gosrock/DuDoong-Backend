package band.gosrock.domain.common.vo

import band.gosrock.common.consts.DuDoongStatic.assetDomain
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable

@Embeddable
class ImageVo(
    var imageKey: String? = null,
) {
    @JsonValue
    fun generateImageUrl(): String? {
        val key = imageKey ?: return null
        // 카카오 이미지로 회원가입한경우 대응
        if (key.contains("kakao")) return key
        // 현재 도메인 대응
        return assetDomain + key
    }

    companion object {
        @JvmStatic
        fun valueOf(key: String?): ImageVo = ImageVo(key)
    }
}
