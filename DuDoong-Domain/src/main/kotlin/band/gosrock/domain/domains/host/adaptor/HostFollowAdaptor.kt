package band.gosrock.domain.domains.host.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.host.repository.HostFollowRepository

@Adaptor
class HostFollowAdaptor(private val hostFollowRepository: HostFollowRepository) {

    fun isFollowing(hostId: Long, userId: Long): Boolean =
        hostFollowRepository.existsByHostIdAndUserId(hostId, userId)

    fun countFollowers(hostId: Long): Long =
        hostFollowRepository.countByHostId(hostId)
}
