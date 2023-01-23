package band.gosrock.domain;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.TestPropertySource;

/** redisson 분산락 Aop 를 중지 시킬 수 있습니다. 테스트 클래스에서 락이 없을때의 실패 테스트를 진행하기 위함. -이찬진 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {"ableRedissonLock=false"})
@Documented
public @interface DisableRedissonLock {}
