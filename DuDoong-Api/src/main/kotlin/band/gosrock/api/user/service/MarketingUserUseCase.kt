package band.gosrock.api.user.service


import band.gosrock.api.common.UserUtils
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.user.service.UserDomainService


@UseCase
class MarketingUserUseCase(
    private val userUtils: UserUtils,
    private val userDomainService: UserDomainService,
) {

    fun execute(): UserInfoVo {
        val currentUser = userUtils.getCurrentUser()
        return currentUser.toUserInfoVo()
    }

    fun toggleMailAgree(): UserInfoVo {
        userDomainService.toggleMailAgree(userUtils.getCurrentUserId())
        return userUtils.getCurrentUser().toUserInfoVo()
    }

    fun toggleMarketAgree(): UserInfoVo {
        userDomainService.toggleMarketAgree(userUtils.getCurrentUserId())
        return userUtils.getCurrentUser().toUserInfoVo()
    }
}
