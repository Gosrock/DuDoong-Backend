package band.gosrock.domain.common.aop.redissonLock

import band.gosrock.common.exception.BadLockIdentifierException
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.common.exception.NotAvailableRedissonLockException
import jakarta.persistence.EntityManagerFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionTimedOutException
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.StringUtils
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit

/**
 * `@RedissonLock` 분산락 AOP. 순서: **락 획득 → 새 트랜잭션([RedissonCallNewTransaction], REQUIRES_NEW) → 메서드 → 커밋 → 락 해제**.
 *
 * 트랜잭션 AOP(`@Transactional`, 기본 order = [Ordered.LOWEST_PRECEDENCE])보다 **바깥**에 둔다 (#743). 같은 order 이면 트랜잭션 AOP 가 바깥이 되어,
 * 메서드·클래스의 `@Transactional` 이 락을 기다리기 전에 커넥션을 잡고(락 대기 중 점유) 락 트랜잭션과 함께 커넥션 2개를 쥐었다.
 * 바깥에 두면 메서드의 `@Transactional` 은 락의 새 트랜잭션에 참여만 한다 (커넥션 1개). 락 트랜잭션은 늘 새 영속성 컨텍스트([withoutOpenEntityManager])
 */
@Aspect
@Component
@Order(RedissonLockAop.ORDER)
@ConditionalOnExpression("\${ableRedissonLock:true}")
class RedissonLockAop(
    private val redissonClient: RedissonClient,
    private val callTransactionFactory: CallTransactionFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(band.gosrock.domain.common.aop.redissonLock.RedissonLock)")
    @Throws(Throwable::class)
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val redissonLock = method.getAnnotation(RedissonLock::class.java)

        val baseKey = redissonLock.LockName
        val dynamicKey = generateDynamicKey(
            redissonLock.identifier,
            joinPoint.args,
            redissonLock.paramClassType.java,  // KClass -> Class
            signature.parameterNames,
        )

        val rLock = redissonClient.getLock("$baseKey:$dynamicKey")
        log.info("redisson 키 설정$baseKey:$dynamicKey")

        val waitTime = redissonLock.waitTime
        val leaseTime = redissonLock.leaseTime
        val timeUnit: TimeUnit = redissonLock.timeUnit

        try {
            val available = rLock.tryLock(waitTime, leaseTime, timeUnit)
            if (!available) throw NotAvailableRedissonLockException.EXCEPTION

            log.info("redisson 락 안으로 진입 $baseKey:$dynamicKey 쓰레드 아이디${Thread.currentThread().id}")
            if (redissonLock.needSameTransaction) return callTransactionFactory.getCallTransaction(true).proceed(joinPoint)
            return withoutOpenEntityManager { callTransactionFactory.getCallTransaction(false).proceed(joinPoint) }
        } catch (e: DuDoongCodeException) {
            throw e
        } catch (e: DuDoongDynamicException) {
            throw e
        } catch (e: TransactionTimedOutException) {
            throw e
        } finally {
            try {
                rLock.unlock()
            } catch (e: IllegalMonitorStateException) {
                log.error("$e$baseKey$dynamicKey")
                throw e
            }
        }
    }

    /**
     * 락 트랜잭션이 **새 영속성 컨텍스트**에서 돌게 한다 (#743). 진행 중인 트랜잭션이 없는데 open-in-view 의 EntityManager 가 스레드에 묶여 있으면
     * REQUIRES_NEW 가 그 EntityManager 를 그대로 써서, 락 전에 읽은 엔티티(락 대기 중 다른 요청이 바꿨을 수 있는 옛 상태)가 락 트랜잭션에 보인다.
     * - `@Transactional` 이 있던 락 메서드: 예전(트랜잭션 AOP 가 바깥)에도 그 트랜잭션을 REQUIRES_NEW 가 보류하며 새 EntityManager 를 썼다 — 같은 결과
     * - `@Transactional` 이 없던 락 메서드(쿠폰·운영 재고 조정·회원 탈퇴 등): 예전에는 open-in-view EntityManager 를 그대로 썼지만 **이제 새 EntityManager** 를 쓴다
     *
     * 떼어 내는 것은 open-in-view 가 묶은 holder(트랜잭션 동기화에 묶이지 않은 것)만이다. SUPPORTS 같은 트랜잭션 동기화 범위가 묶은 holder 는
     * 그 범위가 끝날 때 스스로 풀므로 건드리면 안 된다(떼면 범위 정리 때 IllegalStateException). 진행 중인 트랜잭션이 있으면 REQUIRES_NEW 가 스스로 보류하므로 손대지 않는다.
     * 메서드가 예외로 끝나도 finally 에서 되돌리고 원래 예외를 그대로 던진다
     */
    private fun <T> withoutOpenEntityManager(block: () -> T): T {
        if (TransactionSynchronizationManager.isActualTransactionActive()) return block()
        val held = TransactionSynchronizationManager.getResourceMap().filter { (key, value) ->
            key is EntityManagerFactory && value is EntityManagerHolder && !value.isSynchronizedWithTransaction
        }
        held.keys.forEach { TransactionSynchronizationManager.unbindResource(it) }
        try {
            return block()
        } finally {
            held.forEach { (key, value) -> if (!TransactionSynchronizationManager.hasResource(key)) TransactionSynchronizationManager.bindResource(key, value) }
        }
    }

    fun generateDynamicKey(
        identifier: String,
        args: Array<Any>,
        paramClassType: Class<*>,
        parameterNames: Array<String>,
    ): String {
        return try {
            if (paramClassType == Any::class.java) {
                createDynamicKeyFromPrimitive(parameterNames, args, identifier)
            } else {
                createDynamicKeyFromObject(args, paramClassType, identifier)
            }
        } catch (e: IllegalAccessException) {
            log.error(e.message)
            throw BadLockIdentifierException.EXCEPTION
        } catch (e: NoSuchMethodException) {
            log.error(e.message)
            throw BadLockIdentifierException.EXCEPTION
        } catch (e: InvocationTargetException) {
            log.error(e.message)
            throw BadLockIdentifierException.EXCEPTION
        }
    }

    fun createDynamicKeyFromPrimitive(
        methodParameterNames: Array<String>,
        args: Array<Any>,
        paramName: String,
    ): String {
        for (i in methodParameterNames.indices) {
            if (methodParameterNames[i] == paramName) {
                return args[i].toString()
            }
        }
        throw BadLockIdentifierException.EXCEPTION
    }

    @Throws(IllegalAccessException::class, NoSuchMethodException::class, InvocationTargetException::class)
    fun createDynamicKeyFromObject(
        args: Array<Any>,
        paramClassType: Class<*>,
        identifier: String,
    ): String {
        val paramClassName = paramClassType.simpleName
        for (arg in args) {
            val argsClassName = arg.javaClass.simpleName
            if (argsClassName.startsWith(paramClassName)) {
                val aClass = arg.javaClass
                val capitalize = StringUtils.capitalize(identifier)
                val result = aClass.getMethod("get$capitalize").invoke(arg)
                return result.toString()
            }
        }
        throw BadLockIdentifierException.EXCEPTION
    }

    companion object {
        /** 트랜잭션 AOP(기본 [Ordered.LOWEST_PRECEDENCE]) 바로 바깥 */
        const val ORDER = Ordered.LOWEST_PRECEDENCE - 1
    }
}
