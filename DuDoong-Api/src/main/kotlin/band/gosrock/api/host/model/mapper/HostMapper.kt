package band.gosrock.api.host.model.mapper

import band.gosrock.api.host.model.dto.request.CreateHostRequest
import band.gosrock.api.host.model.dto.request.UpdateHostRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.dto.response.HostMemberResponse
import band.gosrock.api.host.model.dto.response.InviteUserResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostProfile
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import org.springframework.transaction.annotation.Transactional

@Mapper
@Transactional(readOnly = true)
class HostMapper(
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
) {
    fun toEntity(createHostRequest: CreateHostRequest, masterUserId: Long): Host {
        return Host(
            name = createHostRequest.name,
            contactEmail = createHostRequest.contactEmail,
            contactNumber = createHostRequest.contactNumber,
            masterUserId = masterUserId,
        )
    }

    fun toHostProfile(updateHostRequest: UpdateHostRequest): HostProfile {
        return HostProfile(
            introduce = updateHostRequest.introduce,
            profileImageKey = updateHostRequest.profileImageKey,
            contactEmail = updateHostRequest.contactEmail,
            contactNumber = updateHostRequest.contactNumber,
        )
    }

    /** 호스트 역할을 지정하여 주입하는 생성자 */
    fun toHostUser(hostId: Long, userId: Long, role: HostRole): HostUser {
        val host = hostAdaptor.findById(hostId)
        return HostUser(host = host, userId = userId, role = role)
    }

    /** 매니저로 주입하는 생성자 */
    fun toManagerHostUser(hostId: Long, userId: Long): HostUser {
        val host = hostAdaptor.findById(hostId)
        return HostUser(host = host, userId = userId, role = HostRole.MANAGER)
    }

    /** 마스터 주입하는 생성자 */
    fun toMasterHostUser(hostId: Long, userId: Long): HostUser {
        val host = hostAdaptor.findById(hostId)
        return HostUser(host = host, userId = userId, role = HostRole.MASTER)
    }

    fun toHostInviteUserList(hostId: Long, email: String): InviteUserResponse =
        userAdaptor.queryUserByEmail(email).let { inviteUser ->
            val host = hostAdaptor.findById(hostId)
            if (host.hasHostUserId(inviteUser.id!!)) {
                throw AlreadyJoinedHostException.EXCEPTION
            }
            InviteUserResponse.from(inviteUser)
        }

    /** viewerUserId 가 활성 매니저 이상이거나 SUPER_ADMIN 일 때만 slackUrl 을 담는다 (v2 민감 정보 노출 기준과 같음) */
    fun toHostDetailResponse(hostId: Long, viewerUserId: Long): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        return toHostDetailResponse(host, viewerUserId)
    }

    fun toHostDetailResponse(host: Host, viewerUserId: Long): HostDetailResponse {
        val userIds = host.getHostUser_UserIds()
        val userMap = userAdaptor.queryUserListByIdIn(userIds).associateBy { it.id }
        val members = userIds.mapNotNull { userId ->
            userMap[userId]?.let { HostMemberResponse.of(it, host.getHostUserByUserId(userId)) }
        }
        return HostDetailResponse.of(host, members, showSlackUrl = canViewSlackUrl(host, viewerUserId))
    }

    private fun canViewSlackUrl(host: Host, viewerUserId: Long): Boolean {
        val viewerRole = host.getActiveRoleOf(viewerUserId)
        if (viewerRole == HostRole.MASTER || viewerRole == HostRole.MANAGER) return true
        return userAdaptor.queryUser(viewerUserId).accountRole == AccountRole.SUPER_ADMIN
    }
}
