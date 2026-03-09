package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.annotation.JsonUnwrapped

class HostUserVo private constructor(
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

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userInfoVo: UserInfoVo? = null
        private var role: HostRole? = null
        private var active: Boolean? = null

        fun userInfoVo(v: UserInfoVo?) = apply { userInfoVo = v }
        fun role(v: HostRole?) = apply { role = v }
        fun active(v: Boolean?) = apply { active = v }
        fun build() = HostUserVo(userInfoVo!!, role, active)
    }
}
