package band.gosrock.api.image.dto

import band.gosrock.common.annotation.Enum
import band.gosrock.infrastructure.config.s3.ImageFileExtension

data class ImageUrlRequest(
    @field:Enum
    val imageFileExtension: ImageFileExtension,
)
