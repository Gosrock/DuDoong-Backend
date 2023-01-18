package band.gosrock.domain.common.aop.redissonLock;


import band.gosrock.common.exception.BadLockIdentifierException;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.common.exception.NotAvailableRedissonLockException;
import java.lang.reflect.InvocationTargetException;
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
import org.springframework.util.StringUtils;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockAop {
    private final RedissonClient redissonClient;
    private final CallTransactionFactory callTransactionFactory;

    @Around("@annotation(band.gosrock.domain.common.aop.redissonLock.RedissonLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RedissonLock redissonLock = method.getAnnotation(RedissonLock.class);
        String baseKey = redissonLock.LockName();

        String dynamicKey =
                generateDynamicKey(
                        redissonLock.identifier(),
                        joinPoint.getArgs(),
                        redissonLock.paramClassType(),
                        signature.getParameterNames());

        RLock rLock = redissonClient.getLock(baseKey + ":" + dynamicKey);

        log.info("redisson 키 설정" + baseKey + ":" + dynamicKey);

        long waitTime = redissonLock.waitTime();
        long leaseTime = redissonLock.leaseTime();
        TimeUnit timeUnit = redissonLock.timeUnit();
        try {
            boolean available = rLock.tryLock(waitTime, leaseTime, timeUnit);
            if (!available) {
                throw NotAvailableRedissonLockException.EXCEPTION;
            }
            return callTransactionFactory
                    .getCallTransaction(redissonLock.needSameTransaction())
                    .proceed(joinPoint);
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

    public String generateDynamicKey(
            String identifier, Object[] args, Class<?> paramClassType, String[] parameterNames) {
        try {
            String dynamicKey;
            if (paramClassType.equals(Object.class)) {
                dynamicKey = createDynamicKeyFromPrimitive(parameterNames, args, identifier);
            } else {
                dynamicKey = createDynamicKeyFromObject(args, paramClassType, identifier);
            }
            return dynamicKey;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException error) {
            log.error(error.getMessage());
            throw BadLockIdentifierException.EXCEPTION;
        }
    }

    public String createDynamicKeyFromPrimitive(
            String[] methodParameterNames, Object[] args, String paramName) {
        for (int i = 0; i < methodParameterNames.length; i++) {
            if (methodParameterNames[i].equals(paramName)) {
                return String.valueOf(args[i]);
            }
        }
        throw BadLockIdentifierException.EXCEPTION;
    }

    public String createDynamicKeyFromObject(
            Object[] args, Class<?> paramClassType, String identifier)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String name = paramClassType.getSimpleName();
        for (int i = 0; i < args.length; i++) {
            if (args[i].getClass().getSimpleName().equals(name)) {
                Class<?> aClass = args[i].getClass();
                String capitalize = StringUtils.capitalize(identifier);
                Object result = aClass.getMethod("get" + capitalize).invoke(args[i]);
                return String.valueOf(result);
            }
        }
        throw BadLockIdentifierException.EXCEPTION;
    }
}
