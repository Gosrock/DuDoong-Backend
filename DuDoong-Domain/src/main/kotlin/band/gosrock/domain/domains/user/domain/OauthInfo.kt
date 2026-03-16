package band.gosrock.domain.domains.user.domain

import band.gosrock.common.consts.DuDoongStatic
import java.time.LocalDateTime
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class OauthInfo() {
    @Enumerated(EnumType.STRING)
    var provider: OauthProvider? = null
        protected set

    var oid: String? = null
        protected set

    constructor(provider: OauthProvider, oid: String) : this() {
        this.provider = provider
        this.oid = oid
    }

    fun withDrawOauthInfo(): OauthInfo =
        OauthInfo(provider!!, DuDoongStatic.WITHDRAW_PREFIX + LocalDateTime.now() + ":" + oid)

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var provider: OauthProvider? = null
        private var oid: String = ""

        fun provider(provider: OauthProvider) = apply { this.provider = provider }
        fun oid(oid: String) = apply { this.oid = oid }
        fun build() = OauthInfo(provider!!, oid)
    }
}
