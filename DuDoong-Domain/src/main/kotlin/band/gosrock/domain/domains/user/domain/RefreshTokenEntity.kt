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
}
