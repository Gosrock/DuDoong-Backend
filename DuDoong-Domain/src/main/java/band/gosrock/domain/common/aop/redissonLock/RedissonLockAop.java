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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionTimedOutException;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockAop {
    private final RedissonClient redissonClient;
    private final RedissonCallTransaction redissonCallTransaction;
    private final Environment environment;

    @Around("@annotation(band.gosrock.domain.common.aop.redissonLock.RedissonLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RedissonLock redissonLock = method.getAnnotation(RedissonLock.class);
        String baseKey = redissonLock.LockName();

        String DynamicKey =
                generateDynamicKey(
                        redissonLock.identifier(),
                        joinPoint.getArgs(),
                        redissonLock.paramClassType(),
                        signature.getParameterNames());

        RLock rLock = redissonClient.getLock(baseKey + DynamicKey);

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

    private String generateDynamicKey(
            String identifier, Object[] args, Class<?> paramClassType, String[] parameterNames)
            throws NoSuchFieldException {

        String dynamicKey;
        if (paramClassType.equals(Object.class)) {
            dynamicKey = createDynamicKeyFromPrimitive(parameterNames, args, identifier);
        } else {
            dynamicKey = createDynamicKeyFromObject(parameterNames, paramClassType, identifier);
        }
        return dynamicKey;
    }

    private String createDynamicKeyFromPrimitive(
            String[] methodParameterNames, Object[] args, String key) {
        String dynamicKey = "";
        for (int i = 0; i < methodParameterNames.length; i++) {
            if (methodParameterNames[i].equals(key)) {
                dynamicKey += args[i];
                break;
            }
        }
        return dynamicKey;
    }

    private String createDynamicKeyFromObject(Object[] args, Class<?> paramClassType, String key)
            throws NoSuchFieldException {
        String dynamicKey = "";
        String name = paramClassType.getSimpleName();
        for (int i = 0; i < args.length; i++) {
            if (args[i].getClass().getSimpleName().equals(name)) {
                dynamicKey += args[i].getClass().getField(key);
                break;
            }
        }
        return dynamicKey;
    }
}
