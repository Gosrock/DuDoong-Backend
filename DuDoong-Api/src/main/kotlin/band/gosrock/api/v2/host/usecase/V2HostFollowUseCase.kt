package band.gosrock.api.v2.host.usecase

import band.gosrock.api.v2.host.dto.response.V2HostFollowResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.adaptor.HostFollowAdaptor
import band.gosrock.domain.domains.host.service.HostFollowService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException

@UseCase
class V2HostFollowUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostFollowAdaptor: HostFollowAdaptor,
    private val hostFollowService: HostFollowService,
) {
    private val log = LoggerFactory.getLogger(V2HostFollowUseCase::class.java)

    /** 멱등 팔로우 */
    fun follow(userId: Long, hostId: Long): V2HostFollowResponse {
        hostAdaptor.findById(hostId)
        try {
            hostFollowService.follow(hostId, userId)
        } catch (e: DataIntegrityViolationException) {
            // 동시 팔로우로 unique(host_id, user_id) 위반: 이미 팔로우된 상태이므로 성공으로 본다
            log.debug("[V2HostFollowUseCase] 동시 팔로우 중복 무시 hostId={}, userId={}", hostId, userId)
        }
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
