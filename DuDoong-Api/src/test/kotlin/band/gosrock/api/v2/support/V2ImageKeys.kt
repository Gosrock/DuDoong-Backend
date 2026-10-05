package band.gosrock.api.v2.support

import java.util.UUID

/**
 * v2 이미지 key 화이트리스트 테스트용 (#729 리뷰). 발급 형식 = `{prefix}{UUID}.{jpeg|jpg|png}`
 * ([band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService.isUserImageKey] 등)
 */
object V2ImageKeys {

    fun issued(prefix: String, extension: String = "png"): String = "$prefix${UUID.randomUUID()}.$extension"

    /** 거부되어야 하는 key: 경로 조작(평문·인코딩), prefix 만, 다른 확장자, 쿼리스트링, 하위 경로, 카카오·외부 URL, 대문자 UUID, 다른 대상의 prefix */
    fun rejected(prefix: String, otherPrefix: String): List<String> {
        val uuid = UUID.randomUUID().toString()
        return listOf(
            "$prefix../1/$uuid.png",
            "$prefix%2e%2e/1/$uuid.png",
            "$prefix%2E%2E%2F$uuid.png",
            prefix,
            "$prefix.png",
            "$prefix$uuid.html",
            "$prefix$uuid.png.html",
            "$prefix$uuid.gif",
            "$prefix$uuid.png?x",
            "$prefix$uuid.png#x",
            "$prefix$uuid/a.png",
            "${prefix}sub/$uuid.png",
            "$prefix${uuid.uppercase()}.png",
            "${prefix}kakao$uuid.png",
            "http://k.kakaocdn.net/dn/$uuid.png",
            "https://evil.example.com/$uuid.png",
            "$otherPrefix$uuid.png",
        )
    }
}
