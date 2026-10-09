package band.gosrock

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.Environment

@SpringBootApplication
class DuDoongApiServerApplication(
    private val environment: Environment
) : ApplicationListener<ApplicationReadyEvent> {

    private val log = LoggerFactory.getLogger(DuDoongApiServerApplication::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        log.info("applicationReady status${environment.activeProfiles.contentToString()}")
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(DuDoongApiServerApplication::class.java, *args)
}
