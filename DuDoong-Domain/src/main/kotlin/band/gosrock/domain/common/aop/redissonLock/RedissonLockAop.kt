package band.gosrock.domain.common.aop.redissonLock

import band.gosrock.common.exception.BadLockIdentifierException
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.common.exception.NotAvailableRedissonLockException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionTimedOutException
import org.springframework.util.StringUtils
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit

@Aspect
@Component
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
            return callTransactionFactory.getCallTransaction(redissonLock.needSameTransaction).proceed(joinPoint)
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
}
