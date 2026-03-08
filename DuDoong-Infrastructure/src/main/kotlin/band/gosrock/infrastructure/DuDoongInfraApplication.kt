package band.gosrock.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.Environment

// 테스팅 용도 어플리케이션입니다.
@SpringBootApplication
class DuDoongInfraApplication(
    private val environment: Environment,
) : ApplicationListener<ApplicationReadyEvent> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        log.info("applicationReady status" + environment.activeProfiles.contentToString())
    }
}
