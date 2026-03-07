package band.gosrock.infrastructure.config.redis

import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.grid.jcache.JCacheProxyManager
import javax.cache.CacheManager
import javax.cache.Caching
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.jcache.configuration.RedissonConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @Value("\${spring.redis.host}") private val redisHost: String,
    @Value("\${spring.redis.port}") private val redisPort: Int,
) {
    companion object {
        private const val REDISSON_HOST_PREFIX = "redis://"
    }

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.useSingleServer().setAddress("$REDISSON_HOST_PREFIX$redisHost:$redisPort")
        return Redisson.create(config)
    }

    /** for bucket4j */
    @Bean
    fun cacheManager(redissonClient: RedissonClient): CacheManager {
        val manager = Caching.getCachingProvider().cacheManager
        // getCache(String, Class<K>, Class<V>) 오버로드 사용으로 타입 추론 명시
        val bucket4j = manager.getCache("bucket4j", Any::class.java, Any::class.java)
        if (bucket4j == null) {
            manager.createCache("bucket4j", RedissonConfiguration.fromInstance<Any, Any>(redissonClient))
        }
        return manager
    }

    /** for bucket4j */
    @Bean
    fun proxyManager(cacheManager: CacheManager): ProxyManager<String> =
        JCacheProxyManager(cacheManager.getCache("bucket4j", String::class.java, ByteArray::class.java))
}
