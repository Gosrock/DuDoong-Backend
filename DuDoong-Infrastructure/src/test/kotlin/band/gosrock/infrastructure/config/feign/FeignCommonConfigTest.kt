package band.gosrock.infrastructure.config.feign

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import feign.Logger
import feign.Request
import org.slf4j.LoggerFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Feign 로그 (#764)")
class FeignCommonConfigTest {

    private val config = FeignCommonConfig()

    @Test
    fun `기본 로그 레벨은 BASIC (헤더·본문 없음)`() {
        assertEquals(Logger.Level.BASIC, config.feignLoggerLevel())
    }

    @Test
    fun `Authorization 헤더는 레벨과 관계없이 남기지 않는다`() {
        val logger = config.feignLoggerFactory().create(FeignCommonConfigTest::class.java) as FeignCommonConfig.AuthorizationHidingLogger
        assertFalse(logger.shouldLogRequestHeader("Authorization"))
        assertFalse(logger.shouldLogRequestHeader("authorization"))
        assertTrue(logger.shouldLogRequestHeader("Content-Type"))
    }

    @Test
    fun `FULL 로 요청을 남겨도 Authorization 값은 로그에 없다`() {
        val logback = LoggerFactory.getLogger(FeignCommonConfigTest::class.java) as ch.qos.logback.classic.Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        val before = logback.level
        logback.level = Level.DEBUG
        logback.addAppender(appender)
        try {
            val request = Request.create(
                Request.HttpMethod.POST,
                "https://api.tosspayments.com/v1/payments/confirm",
                mapOf("Authorization" to listOf("Basic SECRET_TOSS_KEY"), "Content-Type" to listOf("application/json")),
                ByteArray(0),
                Charsets.UTF_8,
                null,
            )
            val logger = config.feignLoggerFactory().create(FeignCommonConfigTest::class.java)
            val logRequest = Logger::class.java.getDeclaredMethod("logRequest", String::class.java, Logger.Level::class.java, Request::class.java)
            logRequest.isAccessible = true
            logRequest.invoke(logger, "Toss#confirm()", Logger.Level.FULL, request)
            val logged = appender.list.joinToString("\n") { it.formattedMessage }
            assertTrue(logged.contains("Content-Type"), logged)
            assertFalse(logged.contains("SECRET_TOSS_KEY") || logged.contains("Authorization"), logged)
        } finally {
            logback.detachAppender(appender)
            logback.level = before
        }
    }
}
