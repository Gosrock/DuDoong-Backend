package band.gosrock.api.v2.event.dto.response

import band.gosrock.api.v2.event.dto.request.V2EventImagePurpose
import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.infrastructure.config.s3.ImageUrlDto
import io.swagger.v3.oas.annotations.media.Schema

data class V2EventImageUploadResponse(
    val purpose: V2EventImagePurpose,
    @field:Schema(description = "PUT 업로드용 presigned url (3분 유효)")
    val presignedUrl: String,
    @field:Schema(description = "POSTER 면 PATCH /api/v2/events/{eventId}/basic 의 posterImageKey 로 보낼 값")
    val key: String,
    @field:Schema(description = "업로드 완료 후 이미지 url (SECTION 은 본문에 이 url 을 넣는다)")
    val url: String?,
) {
    companion object {
        fun of(purpose: V2EventImagePurpose, urlDto: ImageUrlDto): V2EventImageUploadResponse =
            V2EventImageUploadResponse(
                purpose = purpose,
                presignedUrl = urlDto.url,
                key = urlDto.key,
                url = ImageVo.valueOf(urlDto.key).generateImageUrl(),
            )
    }
}
