package band.gosrock.domain.common.aop.redissonLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import band.gosrock.domain.common.dto.ProfileViewDto;
import band.gosrock.domain.domains.user.domain.Profile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

class RedissonLockAopTest {
    @Mock
    RedissonClient redissonClient;
    @Mock
    RedissonCallTransaction redissonCallTransaction;

    RedissonLockAop redissonLockAop;

    @BeforeEach
    public void beforeEach() {
        redissonLockAop = new RedissonLockAop(redissonClient,
            redissonCallTransaction);
    }

    @Test
    public void testMyMethod()
        throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        ProfileViewDto profileViewDto = ProfileViewDto.builder().id(1000L).build();
        String otherParam = "param";

        Object[] testargs = {profileViewDto,otherParam};
        String[] parameterNames = {"profileViewDto", "otherParam"};
        Class<?> paramClassType = profileViewDto.getClass();
        Object[] args = joinPoint.getArgs();
        String dynamicKey = redissonLockAop.createDynamicKeyFromObject(testargs, paramClassType,
           "id" );
        assertEquals("1000",dynamicKey);
    }

}