package band.gosrock.infrastructure.config.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserRoleCacheService(
    private val redisConnectionFactory: org.springframework.data.redis.connection.RedisConnectionFactory,
) {
    private val redisTemplate: RedisTemplate<String, String> by lazy {
        RedisTemplate<String, String>().apply {
            connectionFactory = redisConnectionFactory
            keySerializer = org.springframework.data.redis.serializer.StringRedisSerializer()
            valueSerializer = org.springframework.data.redis.serializer.StringRedisSerializer()
            afterPropertiesSet()
        }
    }

    companion object {
        private const val KEY_PREFIX = "user:role:"
        private val TTL = Duration.ofMinutes(30)
    }

    fun getRole(userId: Long): String? =
        redisTemplate.opsForValue().get("$KEY_PREFIX$userId")

    fun cacheRole(userId: Long, role: String) {
        redisTemplate.opsForValue().set("$KEY_PREFIX$userId", role, TTL)
    }

    fun evict(userId: Long) {
        redisTemplate.delete("$KEY_PREFIX$userId")
    }
}
