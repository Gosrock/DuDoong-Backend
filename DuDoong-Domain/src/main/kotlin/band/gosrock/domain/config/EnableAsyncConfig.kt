package band.gosrock.domain.config

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@Configuration
class EnableAsyncConfig(
    private val customAsyncExceptionHandler: CustomAsyncExceptionHandler
) : AsyncConfigurer {

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        customAsyncExceptionHandler
}
