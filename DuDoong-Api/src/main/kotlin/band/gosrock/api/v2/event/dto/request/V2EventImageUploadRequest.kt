package band.gosrock.api.v2.event.dto.request

import band.gosrock.infrastructure.config.s3.ImageFileExtension
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class V2EventImageUploadRequest(
    @field:Schema(description = "이미지 용도", example = "POSTER")
    @field:NotNull
    val purpose: V2EventImagePurpose?,

    @field:Schema(description = "확장자", example = "PNG")
    @field:NotNull
    val extension: ImageFileExtension?,
)

enum class V2EventImagePurpose {
    POSTER,
    SECTION,
}
