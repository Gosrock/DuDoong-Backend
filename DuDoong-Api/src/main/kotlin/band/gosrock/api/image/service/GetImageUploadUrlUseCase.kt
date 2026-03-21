package band.gosrock.api.image.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.image.dto.ImageUrlResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.infrastructure.config.s3.ImageFileExtension
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService

@UseCase
class GetImageUploadUrlUseCase(
    private val presignedUrlService: S3UploadPresignedUrlService,
) {
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    fun forEvent(userId: Long, eventId: Long, imageFileExtension: ImageFileExtension): ImageUrlResponse =
        ImageUrlResponse.from(presignedUrlService.forEvent(eventId, imageFileExtension))

    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun forHost(userId: Long, hostId: Long, imageFileExtension: ImageFileExtension): ImageUrlResponse =
        ImageUrlResponse.from(presignedUrlService.forHost(hostId, imageFileExtension))
}
