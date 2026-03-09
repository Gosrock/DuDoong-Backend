package band.gosrock.domain.domains.user.domain

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed

@RedisHash(value = "refreshToken")
class RefreshTokenEntity(
    @Id var id: Long? = null,
    @Indexed var refreshToken: String? = null,
    @TimeToLive var ttl: Long? = null,
) {
    fun updateTTL(ttl: Long) {
        this.ttl = (this.ttl ?: 0) + ttl
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var id: Long? = null
        private var refreshToken: String? = null
        private var ttl: Long? = null

        fun id(id: Long?) = apply { this.id = id }
        fun refreshToken(refreshToken: String?) = apply { this.refreshToken = refreshToken }
        fun ttl(ttl: Long?) = apply { this.ttl = ttl }
        fun build() = RefreshTokenEntity(id, refreshToken, ttl)
    }
}
