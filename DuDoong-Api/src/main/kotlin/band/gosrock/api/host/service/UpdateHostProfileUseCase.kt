package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.request.UpdateHostRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.exception.InvalidHostImageKeyException
import band.gosrock.domain.domains.host.service.HostService
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateHostProfileUseCase(
    private val hostService: HostService,
    private val hostAdaptor: HostAdaptor,
    private val hostMapper: HostMapper,
    private val presignedUrlService: S3UploadPresignedUrlService,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, updateHostRequest: UpdateHostRequest): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        validateProfileImageKey(host, updateHostRequest.profileImageKey)

        return hostMapper.toHostDetailResponse(
            hostService.updateHostProfile(host, hostMapper.toHostProfile(updateHostRequest)),
            userId,
        )
    }

    /** 지금 저장된 프로필 key 를 그대로 다시 보내는 경우 외에는, 이 호스트에 발급한 형식 그대로의 key 여야 한다 */
    private fun validateProfileImageKey(host: Host, key: String) {
        if (key == host.profile?.profileImage?.imageKey) return
        if (!presignedUrlService.isHostImageKey(host.id!!, key)) {
            throw InvalidHostImageKeyException.EXCEPTION
        }
    }
}
