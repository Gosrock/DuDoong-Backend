package band.gosrock.domain.config

import band.gosrock.infrastructure.config.slack.SlackAsyncErrorSender
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
class CustomAsyncExceptionHandler(
    private val slackAsyncErrorSender: SlackAsyncErrorSender
) : AsyncUncaughtExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handleUncaughtException(throwable: Throwable, method: Method, vararg params: Any?) {
        log.error("Exception message - $throwable")
        log.error("Method name - ${method.name}")
        for (param in params) {
            log.error("Parameter value - $param")
        }
        @Suppress("UNCHECKED_CAST")
        slackAsyncErrorSender.execute(method.name, throwable, params as Array<Any>)
    }
}
