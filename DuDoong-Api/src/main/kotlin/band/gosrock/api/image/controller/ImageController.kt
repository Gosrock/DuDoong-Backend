package band.gosrock.api.image.controller

import band.gosrock.api.image.dto.ImageUrlResponse
import band.gosrock.api.image.service.GetImageUploadUrlUseCase
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.infrastructure.config.s3.ImageFileExtension
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SecurityRequirement(name = "access-token")
@Tag(name = "a1. [이미지]")
@RestController
@RequestMapping("/api/v1")
class ImageController(
    private val getImageUploadUrlUseCase: GetImageUploadUrlUseCase,
) {
    @Operation(summary = "이벤트 관련 이미지 업로드 url 요청할수 있는 api 입니다.")
    @PostMapping(value = ["/events/{eventId}/images"])
    fun getIssuedTickets(
        @CurrentUserId userId: Long,
        @PathVariable eventId: Long,
        @RequestParam imageFileExtension: ImageFileExtension,
    ): ImageUrlResponse = getImageUploadUrlUseCase.forEvent(userId, eventId, imageFileExtension)

    @Operation(summary = "호스트 관련 이미지 업로드 url 요청할수 있는 api 입니다.")
    @PostMapping(value = ["/hosts/{hostId}/images"])
    fun patchIssuedTicketStatus(
        @CurrentUserId userId: Long,
        @PathVariable hostId: Long,
        @RequestParam imageFileExtension: ImageFileExtension,
    ): ImageUrlResponse = getImageUploadUrlUseCase.forHost(userId, hostId, imageFileExtension)
}
