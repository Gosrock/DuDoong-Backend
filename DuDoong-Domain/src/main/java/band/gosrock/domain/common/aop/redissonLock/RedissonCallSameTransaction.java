package band.gosrock.domain.common.aop.redissonLock;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonCallSameTransaction implements CallTransaction {
    // 다른 트랜젹선이 걸린서비스가 해당 조인포인트(메서드를) 호출하더라도
    // 새로운 트랜잭션이 보장되어야합니다. (재고를 감소시키는 로직이므로)
    // leaseTime 보다 트랜잭션 타임아웃을 작게 설정
    // leastTimeOut 발생전에 rollback 시키기 위함
    @Transactional(propagation = Propagation.MANDATORY, timeout = 9)
    public Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
