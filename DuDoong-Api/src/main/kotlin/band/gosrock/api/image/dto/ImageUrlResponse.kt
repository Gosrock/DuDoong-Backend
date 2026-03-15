package band.gosrock.api.image.dto

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.infrastructure.config.s3.ImageUrlDto

data class ImageUrlResponse(
    val presignedUrl: String,
    val key: String,
    val url: ImageVo,
) {
    companion object {
        @JvmStatic
        fun from(urlDto: ImageUrlDto): ImageUrlResponse =
            ImageUrlResponse(
                presignedUrl = urlDto.url,
                key = urlDto.key,
                url = ImageVo.valueOf(urlDto.key),
            )
    }
}
