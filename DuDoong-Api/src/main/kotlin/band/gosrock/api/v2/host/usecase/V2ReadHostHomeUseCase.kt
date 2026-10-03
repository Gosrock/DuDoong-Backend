package band.gosrock.api.v2.host.usecase

import band.gosrock.api.v2.host.dto.response.V2HostHomeResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.adaptor.HostFollowAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadHostHomeUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostFollowAdaptor: HostFollowAdaptor,
) {
    /** 공개 API. 비로그인이면 userId 는 0 */
    @Transactional(readOnly = true)
    fun execute(userId: Long, hostId: Long): V2HostHomeResponse {
        val host = hostAdaptor.findById(hostId)
        return V2HostHomeResponse.of(
            host = host,
            followerCount = hostFollowAdaptor.countFollowers(hostId),
            isFollowing = userId != ANONYMOUS_USER_ID && hostFollowAdaptor.isFollowing(hostId, userId),
            userId = userId,
        )
    }

    companion object {
        private const val ANONYMOUS_USER_ID = 0L
    }
}
