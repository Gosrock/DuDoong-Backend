package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.request.V2HostImageUploadRequest
import band.gosrock.api.v2.host.dto.response.V2HostImageUploadResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService

@UseCase
class V2GetHostImageUploadUrlUseCase(
    private val presignedUrlService: S3UploadPresignedUrlService,
) {
    /** 프로필·커버 모두 기존 호스트 이미지 경로(host/{hostId}/...)를 쓴다. purpose 는 응답에 그대로 돌려준다 */
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: V2HostImageUploadRequest): V2HostImageUploadResponse =
        V2HostImageUploadResponse.of(request.purpose!!, presignedUrlService.forHost(hostId, request.extension!!))
}
