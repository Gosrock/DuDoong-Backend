package band.gosrock.domain.common.vo

import band.gosrock.common.consts.DuDoongStatic.assetDomain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("이미지 URL 생성 - 카카오 CDN 판정 (#764)")
class ImageVoTest {

    @Test
    fun `운영 데이터에 있는 카카오 이미지 주소는 그대로 쓴다`() {
        listOf(
            "http://k.kakaocdn.net/dn/abc/img_640x640.jpg",
            "https://k.kakaocdn.net/dn/abc/img_640x640.jpg",
            "http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=abc",
            "http://t1.kakaocdn.net/account_images/default_profile.jpeg",
            "http://beta-api1-kage.kakao.com/abc.jpg",
        ).forEach { assertEquals(it, ImageVo.valueOf(it).generateImageUrl(), it) }
    }

    @Test
    fun `kakao 가 들어 있어도 카카오 CDN 주소가 아니면 에셋 도메인 key 로 다룬다`() {
        listOf(
            "https://example.com/kakao.png",
            "https://k.kakaocdn.net.example.com/a.jpg",
            "https://example.com/k.kakaocdn.net/a.jpg",
            "production/user/1/kakao.png",
        ).forEach { assertEquals(assetDomain + it, ImageVo.valueOf(it).generateImageUrl(), it) }
    }

    @Test
    fun `S3 key 와 null`() {
        assertEquals("${assetDomain}production/event/1/a.png", ImageVo.valueOf("production/event/1/a.png").generateImageUrl())
        assertNull(ImageVo.valueOf(null).generateImageUrl())
    }
}
