package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminHostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetHostMembersUseCase(
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
) {

    fun execute(hostId: Long): List<AdminHostMemberResponse> {
        val host = hostAdaptor.findById(hostId)
        val userIds = host.getHostUser_UserIds()
        val userMap = userAdaptor.queryUserListByIdIn(userIds)
            .associateBy { it.id }

        return host.hostUsers.map { hostUser ->
            val userName = hostUser.userId?.let { userMap[it]?.profile?.name }
            AdminHostMemberResponse.of(hostUser, userName)
        }
    }
}
