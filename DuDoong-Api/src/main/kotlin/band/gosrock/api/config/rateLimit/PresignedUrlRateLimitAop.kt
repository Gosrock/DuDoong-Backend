package band.gosrock.api.config.rateLimit

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.common.exception.TooManyRequestException
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.proxy.ProxyManager
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 이미지 업로드 presigned URL 발급 횟수 제한 (#764). 요청한 유저별로 분당 [perMinute] 회.
 * presigned PUT 은 파일 크기를 서명에 묶을 수 없어 발급 횟수로 업로드 양을 묶는다. v1·v2 모든 발급 경로가 S3UploadPresignedUrlService 의 for* 를 거치므로 그 메서드에 건다
 */
@Aspect
@Component
class PresignedUrlRateLimitAop(
    private val buckets: ProxyManager<String>,
    @Value("\${throttle.presigned-url-per-minute}") private val perMinute: Long,
) {
    @Around("execution(* band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService.for*(..))")
    fun limit(joinPoint: ProceedingJoinPoint): Any? {
        val userId = SecurityUtils.getCurrentUserId()
        val config = BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(perMinute, Refill.greedy(perMinute, Duration.ofMinutes(1))))
            .build()
        if (!buckets.builder().build("presigned-url:$userId") { config }.tryConsume(1)) {
            throw TooManyRequestException.EXCEPTION
        }
        return joinPoint.proceed()
    }
}
