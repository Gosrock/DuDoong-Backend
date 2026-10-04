package band.gosrock.api.v2.host.dto.request

import band.gosrock.infrastructure.config.s3.ImageFileExtension
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class V2HostImageUploadRequest(
    @field:Schema(description = "이미지 용도", example = "PROFILE")
    @field:NotNull
    val purpose: V2HostImagePurpose?,

    @field:Schema(description = "확장자", example = "PNG")
    @field:NotNull
    val extension: ImageFileExtension?,
)

enum class V2HostImagePurpose {
    PROFILE,
    COVER,
}
