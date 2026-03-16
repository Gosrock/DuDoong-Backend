package band.gosrock.api.user.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.UserInfoVo
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.service.UserDomainService

@UseCase
class MarketingUserUseCase(
    private val userAdaptor: UserAdaptor,
    private val userDomainService: UserDomainService,
) {

    fun execute(userId: Long): UserInfoVo {
        val currentUser = userAdaptor.queryUser(userId)
        return currentUser.toUserInfoVo()
    }

    fun toggleMailAgree(userId: Long): UserInfoVo {
        userDomainService.toggleMailAgree(userId)
        return userAdaptor.queryUser(userId).toUserInfoVo()
    }

    fun toggleMarketAgree(userId: Long): UserInfoVo {
        userDomainService.toggleMarketAgree(userId)
        return userAdaptor.queryUser(userId).toUserInfoVo()
    }
}
