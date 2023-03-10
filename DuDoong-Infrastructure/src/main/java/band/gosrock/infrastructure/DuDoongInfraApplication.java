package band.gosrock.infrastructure;


import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

// 테스팅 용도 어플리케이션입니다.
@SpringBootApplication
// @Profile({"dev","common","infrastructure"})
@RequiredArgsConstructor
@Slf4j
public class DuDoongInfraApplication implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment environment;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("applicationReady status" + Arrays.toString(environment.getActiveProfiles()));
    }
}
