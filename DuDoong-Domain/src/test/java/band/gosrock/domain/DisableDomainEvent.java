package band.gosrock.domain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


/**
 * 도메인 이벤트의 발행을 중지 시킬 수 있습니다.
 * -이찬진
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(
    properties = {"ableDomainEvent=false"}
)
@Documented
public @interface DisableDomainEvent {
}