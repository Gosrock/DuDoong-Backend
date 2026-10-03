package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.request.V2UpdateHostRequest
import band.gosrock.api.v2.host.dto.response.V2HostHomeResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.adaptor.HostFollowAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2UpdateHostUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val hostFollowAdaptor: HostFollowAdaptor,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, request: V2UpdateHostRequest): V2HostHomeResponse {
        val host = hostAdaptor.findById(hostId)
        host.updateProfileV2(
            name = request.name?.trim(),
            introduce = request.introduce,
            profileImageKey = request.profileImageKey,
            coverImageKey = request.coverImageKey,
        )
        request.contacts?.let { contacts -> host.replaceContacts(contacts.map { it.toEntity() }) }
        hostRepository.save(host)
        return V2HostHomeResponse.of(
            host = host,
            followerCount = hostFollowAdaptor.countFollowers(hostId),
            isFollowing = hostFollowAdaptor.isFollowing(hostId, userId),
            userId = userId,
        )
    }
}
