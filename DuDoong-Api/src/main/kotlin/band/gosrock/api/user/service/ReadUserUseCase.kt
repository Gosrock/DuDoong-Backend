package band.gosrock.api.user.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.user.adaptor.UserAdaptor

@UseCase
class ReadUserUseCase(
    private val userAdaptor: UserAdaptor,
) {

    fun execute(userId: Long): UserInfoVo {
        val currentUser = userAdaptor.queryUser(userId)
        return currentUser.toUserInfoVo()
    }
}
