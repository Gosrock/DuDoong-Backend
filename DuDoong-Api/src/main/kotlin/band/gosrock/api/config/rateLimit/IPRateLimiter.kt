package band.gosrock.api.config.rateLimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.proxy.ProxyManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.function.Supplier

@Component
class IPRateLimiter(
    private val buckets: ProxyManager<String>,
) {
    @Value("\${throttle.overdraft}")
    private var overdraft: Long = 0

    @Value("\${throttle.greedyRefill}")
    private var greedyRefill: Long = 0

    fun resolveBucket(key: String): Bucket {
        val configSupplier = getConfigSupplierForUser()
        return buckets.builder().build(key, configSupplier)
    }

    private fun getConfigSupplierForUser(): Supplier<BucketConfiguration> {
        val refill = Refill.greedy(greedyRefill, Duration.ofMinutes(1))
        val limit = Bandwidth.classic(overdraft, refill)
        return Supplier { BucketConfiguration.builder().addLimit(limit).build() }
    }
}
