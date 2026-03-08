package band.gosrock.domain.common.aop.redissonLock

import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/** Redisson 을 활용한 분산락을 걸 메소드에 다는 어노테이션 입니다. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedissonLock(
    // 분산락을 걸 파라미터 네임
    val identifier: String,
    // 락 이름
    val LockName: String,
    val paramClassType: KClass<*> = Any::class,
    val needSameTransaction: Boolean = false,
    // redisson default waitTime 이 30 s 임
    val waitTime: Long = 10L,
    val leaseTime: Long = 10L,
    // 초단위 계산
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
)
