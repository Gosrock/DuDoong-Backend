package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.request.V2UpdateHostRequest
import band.gosrock.api.v2.host.dto.response.V2HostHomeResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.adaptor.HostFollowAdaptor
import band.gosrock.domain.domains.host.exception.InvalidHostImageKeyException
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.host.service.v2.V2HostDomainService
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2UpdateHostUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val hostFollowAdaptor: HostFollowAdaptor,
    private val presignedUrlService: S3UploadPresignedUrlService,
    private val v2HostDomainService: V2HostDomainService,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: V2UpdateHostRequest): V2HostHomeResponse {
        validateImageKey(hostId, request.profileImageKey)
        validateImageKey(hostId, request.coverImageKey)
        val host = hostAdaptor.findById(hostId)
        v2HostDomainService.updateProfile(
            host = host,
            name = request.name?.trim(),
            introduce = request.introduce,
            profileImageKey = request.profileImageKey,
            coverImageKey = request.coverImageKey,
        )
        request.contacts?.let { contacts -> v2HostDomainService.replaceContacts(host, contacts.map { it.toEntity() }) }
        hostRepository.save(host)
        return V2HostHomeResponse.of(
            host = host,
            contacts = v2HostDomainService.displayContacts(host),
            followerCount = hostFollowAdaptor.countFollowers(hostId),
            isFollowing = hostFollowAdaptor.isFollowing(hostId, userId),
            userId = userId,
        )
    }

    /** 빈 문자열(기본 이미지)이거나, H-15 가 이 호스트에 발급한 형식 그대로의 key(prefix + UUID + jpeg/jpg/png)여야 한다. 외부 URL / 다른 호스트 key / 경로 조작 거부 */
    private fun validateImageKey(hostId: Long, key: String?) {
        if (key == null || key.isEmpty()) return
        if (!presignedUrlService.isHostImageKey(hostId, key)) {
            throw InvalidHostImageKeyException.EXCEPTION
        }
    }
}
