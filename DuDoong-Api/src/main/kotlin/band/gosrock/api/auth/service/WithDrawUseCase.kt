package band.gosrock.api.auth.service

import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.service.UserDomainService
import org.springframework.transaction.annotation.Transactional

@UseCase
class WithDrawUseCase(
    private val refreshTokenAdaptor: RefreshTokenAdaptor,
    private val userDomainService: UserDomainService,
    private val userAdaptor: UserAdaptor,
    private val kakaoOauthHelper: KakaoOauthHelper
) {

    @Transactional
    fun execute() {
        val currentUserId = SecurityUtils.getCurrentUserId()
        refreshTokenAdaptor.deleteByUserId(currentUserId)
        val user = userAdaptor.queryUser(currentUserId)
        val oid = user.oauthInfo!!.oid!!
        userDomainService.withDrawUser(currentUserId)
        kakaoOauthHelper.unlink(oid)
    }
}
