package band.gosrock.api.v2.notification

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.notification.handler.V2NotificationAsyncConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@DisplayName("v2 알림센터 - 전용 executor")
class V2NotificationExecutorTest {

    @Test
    fun `가득 차면(실행 4 + 큐 200) 이후 작업은 버린다 - 예외 없음, 호출 스레드에서 실행 안 함`() {
        val executor = V2NotificationAsyncConfig().notificationExecutor().apply { initialize() }
        val release = CountDownLatch(1)
        val ran = AtomicInteger()
        val callerThread = Thread.currentThread()
        val ranOnCaller = AtomicInteger()
        try {
            val capacity = V2NotificationAsyncConfig.MAX_POOL_SIZE + V2NotificationAsyncConfig.QUEUE_CAPACITY
            repeat(capacity + 5) {
                executor.execute {
                    if (Thread.currentThread() == callerThread) ranOnCaller.incrementAndGet()
                    release.await(5, TimeUnit.SECONDS)
                    ran.incrementAndGet()
                }
            }
            assertEquals(0, ranOnCaller.get())
            release.countDown()
            executor.threadPoolExecutor.shutdown()
            assertTrue(executor.threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS))
            assertEquals(capacity, ran.get())
        } finally {
            release.countDown()
            executor.shutdown()
        }
    }

    @Nested
    @ApiIntegrateSpringBootTest
    inner class Context {
        @Autowired private lateinit var context: ApplicationContext

        @Autowired @Qualifier(V2NotificationAsyncConfig.NOTIFICATION_EXECUTOR)
        private lateinit var notificationExecutor: ThreadPoolTaskExecutor

        @Test
        fun `알림 executor 설정, Boot 기본 applicationTaskExecutor 유지`() {
            assertEquals(2, notificationExecutor.corePoolSize)
            assertEquals(4, notificationExecutor.maxPoolSize)
            assertEquals(200, notificationExecutor.queueCapacity)
            assertTrue(notificationExecutor.threadNamePrefix.startsWith("notification-"))
            assertTrue(context.containsBean("applicationTaskExecutor"))
            assertFalse(context.getBean("applicationTaskExecutor") === notificationExecutor)
        }
    }
}
