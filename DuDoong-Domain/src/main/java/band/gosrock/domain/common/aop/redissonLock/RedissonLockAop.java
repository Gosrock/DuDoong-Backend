package band.gosrock.domain.common.aop.redissonLock;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.common.exception.NotAvailableRedissonLockException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionTimedOutException;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockAop {
    private final RedissonClient redissonClient;
    private final RedissonCallTransaction redissonCallTransaction;

    @Around("@annotation(band.gosrock.domain.common.aop.redissonLock.RedissonLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RedissonLock redissonLock = method.getAnnotation(RedissonLock.class);
        String key = this.createKey(signature.getParameterNames(), joinPoint.getArgs(), redissonLock.key());
        RLock rLock = redissonClient.getLock(key);

        long waitTime = redissonLock.waitTime();
        long leaseTime = redissonLock.leaseTime();
        TimeUnit timeUnit = redissonLock.timeUnit();
        try {
            boolean available = rLock.tryLock(waitTime, leaseTime, timeUnit);
            if (!available) {
                throw NotAvailableRedissonLockException.EXCEPTION;
            }
            return redissonCallTransaction.proceed(joinPoint);
        } catch (DuDoongCodeException | DuDoongDynamicException | TransactionTimedOutException e) {
            throw e;
        } finally {
            try {
                rLock.unlock();
            } catch (IllegalMonitorStateException e) {
                rLock.forceUnlock();
            }
        }
    }

    /**
     * Redisson Key Create
     * @param parameterNames
     * @param args
     * @param key
     * @return
     */
    private String createKey(String[] parameterNames, Object[] args, String key) {
        String resultKey = key;

        /* key = parameterName */
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(key)) {
                resultKey += args[i];
                break;
            }
        }

        return resultKey;
    }
}
