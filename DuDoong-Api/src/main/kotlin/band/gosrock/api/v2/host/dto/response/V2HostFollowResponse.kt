package band.gosrock.api.v2.host.dto.response

data class V2HostFollowResponse(
    val hostId: Long,
    val isFollowing: Boolean,
    val followerCount: Long,
)
