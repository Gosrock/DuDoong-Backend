package band.gosrock.api.config.rateLimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IPRateLimiter {
    //autowiring dependencies
    private final ProxyManager<String> buckets;

    private final static long overdraft = 50;

    public Bucket resolveBucket(String key) {
        Supplier<BucketConfiguration> configSupplier = getConfigSupplierForUser(key);
        return buckets.builder().build(key, configSupplier);
    }
    private Supplier<BucketConfiguration> getConfigSupplierForUser(String key) {
        Refill refill = Refill.greedy(30, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(overdraft, refill);
        return () -> (BucketConfiguration.builder()
            .addLimit(limit)
            .build());
    }
}
