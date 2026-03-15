package band.gosrock.api.user.service


import band.gosrock.api.common.UserUtils
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.UserInfoVo


@UseCase
class ReadUserUseCase(
    private val userUtils: UserUtils,
) {

    fun execute(): UserInfoVo {
        val currentUser = userUtils.getCurrentUser()
        return currentUser.toUserInfoVo()
    }
}
