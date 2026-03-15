package band.gosrock.api.host.model.mapper

import band.gosrock.api.host.model.dto.request.CreateHostRequest
import band.gosrock.api.host.model.dto.request.UpdateHostRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.common.vo.HostUserVo
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostProfile
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@Mapper
@Transactional(readOnly = true)
class HostMapper(
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
) {
    fun toEntity(createHostRequest: CreateHostRequest, masterUserId: Long): Host {
        return Host.builder()
            .name(createHostRequest.name)
            .contactEmail(createHostRequest.contactEmail)
            .contactNumber(createHostRequest.contactNumber)
            .masterUserId(masterUserId)
            .build()
    }

    fun toHostProfile(updateHostRequest: UpdateHostRequest): HostProfile {
        return HostProfile.builder()
            .introduce(updateHostRequest.introduce)
            .profileImageKey(updateHostRequest.profileImageKey)
            .contactEmail(updateHostRequest.contactEmail)
            .contactNumber(updateHostRequest.contactNumber)
            .build()
    }

    /** 호스트 역할을 지정하여 주입하는 생성자 */
    fun toHostUser(hostId: Long, userId: Long, role: HostRole): HostUser {
        val host = hostAdaptor.findById(hostId)
        return HostUser.builder().userId(userId).host(host).role(role).build()
    }

    /** 매니저로 주입하는 생성자 */
    fun toManagerHostUser(hostId: Long, userId: Long): HostUser {
        val host = hostAdaptor.findById(hostId)
        return HostUser.builder().userId(userId).host(host).role(HostRole.MANAGER).build()
    }

    /** 마스터 주입하는 생성자 */
    fun toMasterHostUser(hostId: Long, userId: Long): HostUser {
        val host = hostAdaptor.findById(hostId)
        return HostUser.builder().userId(userId).host(host).role(HostRole.MASTER).build()
    }

    fun toHostInviteUserList(hostId: Long, email: String) =
        userAdaptor.queryUserByEmail(email).let { inviteUser ->
            val host = hostAdaptor.findById(hostId)
            if (host.hasHostUserId(inviteUser.id!!)) {
                throw AlreadyJoinedHostException.EXCEPTION
            }
            inviteUser.toUserProfileVo()
        }

    fun toHostDetailResponse(hostId: Long): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        return toHostDetailResponseExecute(host)
    }

    fun toHostDetailResponse(host: Host): HostDetailResponse {
        return toHostDetailResponseExecute(host)
    }

    private fun toHostDetailResponseExecute(host: Host): HostDetailResponse {
        val userIds = host.getHostUser_UserIds()
        val userList = userAdaptor.queryUserListByIdIn(userIds)
        val userMap = userList.associateBy { it.id }
        val hostUserVoList = mutableListOf<HostUserVo>()

        for (userId in userIds) {
            val user = userMap[userId]
            if (user != null) {
                val userInfoVo: UserInfoVo = user.toUserInfoVo()
                val hostUser = host.getHostUserByUserId(userId)
                val hostUserVo = HostUserVo.from(userInfoVo, hostUser)
                hostUserVoList.add(hostUserVo)
            }
        }

        return HostDetailResponse.of(host, hostUserVoList)
    }
}
