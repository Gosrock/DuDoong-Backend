package band.gosrock.api.image.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.image.dto.ImageUrlResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetImageUploadUrlUseCase {

    private final S3UploadPresignedUrlService presignedUrlService;

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public ImageUrlResponse forEvent(Long eventId) {

        return ImageUrlResponse.from(presignedUrlService.forEvent(eventId, "jpeg"));
    }

    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    public ImageUrlResponse forHost(Long hostId) {

        return ImageUrlResponse.from(presignedUrlService.forEvent(hostId, "jpeg"));
    }
}
