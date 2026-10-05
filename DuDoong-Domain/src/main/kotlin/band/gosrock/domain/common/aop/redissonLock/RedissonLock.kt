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
    /**
     * true 면 새 트랜잭션을 열지 않고 **호출 측 트랜잭션에 참여**한다(MANDATORY) — 호출 측 트랜잭션이 반드시 있어야 하며 없으면 IllegalTransactionStateException.
     * 이때 락은 메서드가 끝나면(호출 측 커밋 전에) 풀린다. 기본 false = 락 획득 뒤 새 트랜잭션(REQUIRES_NEW), 락 해제는 그 커밋 뒤 (#743)
     */
    val needSameTransaction: Boolean = false,
    // redisson default waitTime 이 30 s 임
    val waitTime: Long = 10L,
    val leaseTime: Long = 10L,
    // 초단위 계산
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
)
