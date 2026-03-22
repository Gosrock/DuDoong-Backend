package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.annotation.JsonUnwrapped

class HostUserVo(
    @JsonUnwrapped val userInfoVo: UserInfoVo,
    val role: HostRole?,
    val active: Boolean?,
) {
    companion object {
        @JvmStatic
        fun from(user: User, hostUser: HostUser): HostUserVo =
            HostUserVo(
                userInfoVo = user.toUserInfoVo(),
                active = hostUser.active,
                role = hostUser.role,
            )

        @JvmStatic
        fun from(userInfoVo: UserInfoVo, hostUser: HostUser): HostUserVo =
            HostUserVo(
                userInfoVo = userInfoVo,
                active = hostUser.active,
                role = hostUser.role,
            )
    }
}
