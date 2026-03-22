package band.gosrock.domain.domains.user.domain

import band.gosrock.common.consts.DuDoongStatic
import java.time.LocalDateTime
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class OauthInfo(
    @Enumerated(EnumType.STRING)
    var provider: OauthProvider? = null,
    var oid: String? = null,
) {
    fun withDrawOauthInfo(): OauthInfo =
        OauthInfo(provider!!, DuDoongStatic.WITHDRAW_PREFIX + LocalDateTime.now() + ":" + oid)
}
