package band.gosrock.api.v2.host.usecase

import band.gosrock.api.v2.host.dto.response.V2HostFollowResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.adaptor.HostFollowAdaptor
import band.gosrock.domain.domains.host.service.HostFollowService

@UseCase
class V2HostFollowUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostFollowAdaptor: HostFollowAdaptor,
    private val hostFollowService: HostFollowService,
) {
    /** 멱등 팔로우 */
    fun follow(userId: Long, hostId: Long): V2HostFollowResponse {
        hostAdaptor.findById(hostId)
        hostFollowService.follow(hostId, userId)
        return toResponse(hostId, isFollowing = true)
    }

    /** 멱등 언팔로우 */
    fun unfollow(userId: Long, hostId: Long): V2HostFollowResponse {
        hostAdaptor.findById(hostId)
        hostFollowService.unfollow(hostId, userId)
        return toResponse(hostId, isFollowing = false)
    }

    private fun toResponse(hostId: Long, isFollowing: Boolean) =
        V2HostFollowResponse(hostId = hostId, isFollowing = isFollowing, followerCount = hostFollowAdaptor.countFollowers(hostId))
}
