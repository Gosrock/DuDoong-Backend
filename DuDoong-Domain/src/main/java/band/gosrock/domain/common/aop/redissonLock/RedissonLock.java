package band.gosrock.domain.common.aop.redissonLock;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/** Redisson 을 활용한 분산락을 걸 메소드에 다는 어노테이션 입니다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedissonLock {
    // 분산락을 걸 파라미터 네임
    String identifier();

    // 락 이름
    String LockName();

    Class<?> paramClassType() default Object.class;

    // redisson default waitTime 이 30 s 임
    long waitTime() default 10L;

    long leaseTime() default 10L;
    // 초단위 계산
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
