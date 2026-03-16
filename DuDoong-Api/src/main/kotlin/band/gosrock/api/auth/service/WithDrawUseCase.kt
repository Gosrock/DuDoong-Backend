package band.gosrock.api.auth.service

import band.gosrock.api.auth.service.helper.KakaoOauthHelper
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
    fun execute(userId: Long) {
        refreshTokenAdaptor.deleteByUserId(userId)
        val user = userAdaptor.queryUser(userId)
        val oid = user.oauthInfo!!.oid!!
        userDomainService.withDrawUser(userId)
        kakaoOauthHelper.unlink(oid)
    }
}
