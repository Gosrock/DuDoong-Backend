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
        // 카카오 이미지로 회원가입한경우 대응 — 카카오 CDN 주소일 때만 그대로 쓴다 (#764)
        if (isKakaoImageUrl(key)) return key
        // 현재 도메인 대응
        return assetDomain + key
    }

    companion object {
        /**
         * 카카오 프로필 이미지 주소 형식 (#764). 운영 데이터에 있는 호스트: k·img1·t1.kakaocdn.net, 예전 beta-api1-kage.kakao.com.
         * 회원가입 요청 DTO 검증(@Pattern)과 같은 규칙
         */
        const val KAKAO_IMAGE_URL_PATTERN = "^https?://(k|img1|t1)\\.kakaocdn\\.net/\\S+$|^http://beta-api1-kage\\.kakao\\.com/\\S+$"
        private val KAKAO_IMAGE_URL = Regex(KAKAO_IMAGE_URL_PATTERN)

        @JvmStatic
        fun valueOf(key: String?): ImageVo = ImageVo(key)

        @JvmStatic
        fun isKakaoImageUrl(key: String): Boolean = KAKAO_IMAGE_URL.matches(key)
    }
}
