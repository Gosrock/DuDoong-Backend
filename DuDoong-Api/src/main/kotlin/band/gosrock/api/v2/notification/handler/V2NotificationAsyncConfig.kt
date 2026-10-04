package band.gosrock.api.v2.notification.handler

import band.gosrock.domain.config.MdcTaskDecorator
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.RejectedExecutionHandler

/**
 * 알림 저장 전용 executor (#714). 기존 `@Async` 기본 풀(`EnableAsyncConfig`, 슬랙·메일·알림톡 등)과 분리해,
 * 알림이 몰려도 기존 비동기 작업의 큐를 차지하지 않게 한다. 큐가 차면 버리고 warn 로그 (호출 스레드에서 실행하지 않음 — 요청 스레드 지연 방지).
 */
@Configuration
class V2NotificationAsyncConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(NOTIFICATION_EXECUTOR)
    fun notificationExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = CORE_POOL_SIZE
        maxPoolSize = MAX_POOL_SIZE
        queueCapacity = QUEUE_CAPACITY
        setThreadNamePrefix("notification-")
        setTaskDecorator(MdcTaskDecorator())
        setRejectedExecutionHandler(discardWithWarn())
    }

    /**
     * Spring Boot 는 `Executor` 빈이 하나도 없을 때만 `applicationTaskExecutor` 를 만든다 (TaskExecutionAutoConfiguration).
     * [notificationExecutor] 를 빈으로 등록하면 그 기본 빈이 사라지므로, Boot 와 같은 방식(builder, spring.task.execution.*)으로 그대로 유지한다
     */
    @Lazy
    @Bean(name = ["applicationTaskExecutor", "taskExecutor"])
    @ConditionalOnMissingBean(name = ["applicationTaskExecutor"])
    fun applicationTaskExecutor(builder: ThreadPoolTaskExecutorBuilder): ThreadPoolTaskExecutor = builder.build()

    private fun discardWithWarn() = RejectedExecutionHandler { _, executor ->
        log.warn("[알림센터] 알림 저장 작업 버림 (큐 가득 참: active={}, queue={})", executor.activeCount, executor.queue.size)
    }

    companion object {
        const val NOTIFICATION_EXECUTOR = "notificationExecutor"
        const val CORE_POOL_SIZE = 2
        const val MAX_POOL_SIZE = 4
        const val QUEUE_CAPACITY = 200
    }
}
