package band.gosrock.api.v2.host.dto.response

import band.gosrock.api.v2.host.dto.request.V2HostImagePurpose
import band.gosrock.infrastructure.config.s3.ImageUrlDto
import band.gosrock.domain.common.vo.ImageVo
import io.swagger.v3.oas.annotations.media.Schema

data class V2HostImageUploadResponse(
    val purpose: V2HostImagePurpose,
    @field:Schema(description = "PUT 업로드용 presigned url (3분 유효)")
    val presignedUrl: String,
    @field:Schema(description = "업로드 후 PATCH /api/v2/hosts/{hostId} 의 profileImageKey / coverImageKey 로 보낼 값")
    val key: String,
    @field:Schema(description = "업로드 완료 후 이미지 url")
    val url: String?,
) {
    companion object {
        fun of(purpose: V2HostImagePurpose, urlDto: ImageUrlDto): V2HostImageUploadResponse =
            V2HostImageUploadResponse(
                purpose = purpose,
                presignedUrl = urlDto.url,
                key = urlDto.key,
                url = ImageVo.valueOf(urlDto.key).generateImageUrl(),
            )
    }
}
