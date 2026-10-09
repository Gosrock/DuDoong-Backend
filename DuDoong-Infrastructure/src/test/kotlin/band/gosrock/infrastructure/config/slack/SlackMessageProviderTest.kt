package band.gosrock.infrastructure.config.slack

import com.slack.api.Slack
import com.slack.api.webhook.Payload
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("호스트 슬랙 웹훅 URL 검증·리다이렉트 차단 (#764)")
class SlackMessageProviderTest {

    private val provider = SlackMessageProvider("DuDoongBot", "")

    @Test
    fun `Slack Incoming Webhook 형식만 허용한다`() {
        assertTrue(SlackMessageProvider.isSlackWebhookUrl("https://hooks.slack.com/services/T000/B000/XXXX"))
        listOf(
            "http://hooks.slack.com/services/T000/B000/XXXX",
            "https://hooks.slack.com.example.com/services/T000",
            "https://example.com/hooks.slack.com/services/T000",
            "https://hooks.slack.com/workflows/T000",
            "https://hooks.slack.com/services/",
            "https://hooks.slack.com/services/T000?x=1",
            "https://user@hooks.slack.com/services/T000",
            "http://127.0.0.1:8080/",
        ).forEach { assertFalse(SlackMessageProvider.isSlackWebhookUrl(it), it) }
    }

    @Test
    fun `등록은 형식이 아니면 요청을 보내지 않고 UnknownHostException (유스케이스가 InvalidSlackUrl 로 바꾼다)`() {
        val hits = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { ex -> hits.incrementAndGet(); ex.sendResponseHeaders(200, 2); ex.responseBody.use { it.write("ok".toByteArray()) } }
            start()
        }
        try {
            assertThrows(UnknownHostException::class.java) { provider.register("http://127.0.0.1:${server.address.port}/services/x") }
            // 저장돼 있던 비정상 URL 은 보내지 않고 건너뛴다 (예외 없음)
            assertDoesNotThrow { provider.sendMessage("http://127.0.0.1:${server.address.port}/services/x", "알림") }
            assertEquals(0, hits.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `형식이 아닌 저장 URL 을 건너뛸 때 WARN 로그에 hostId 를 남기고 URL 은 남기지 않는다`() {
        val logger = org.slf4j.LoggerFactory.getLogger(SlackMessageProvider::class.java) as ch.qos.logback.classic.Logger
        val appender = ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        try {
            provider.sendMessage("http://127.0.0.1:1/services/secret-path", "알림", 42L)
            val warn = appender.list.single { it.level == ch.qos.logback.classic.Level.WARN }.formattedMessage
            assertTrue(warn.contains("hostId=42"), warn)
            assertFalse(warn.contains("secret-path"), warn)
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `웹훅 전송 클라이언트는 리다이렉트를 따라가지 않는다`() {
        val followed = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/start") { ex ->
                ex.responseHeaders.add("Location", "/next")
                ex.sendResponseHeaders(302, -1)
                ex.close()
            }
            createContext("/next") { ex -> followed.incrementAndGet(); ex.sendResponseHeaders(200, 2); ex.responseBody.use { it.write("ok".toByteArray()) } }
            start()
        }
        try {
            val slack = SlackMessageProvider::class.java.getDeclaredField("slack").apply { isAccessible = true }.get(provider) as Slack
            val response = slack.send("http://127.0.0.1:${server.address.port}/start", Payload.builder().text("t").build())
            assertEquals(302, response.code)
            assertEquals(0, followed.get())
        } finally {
            server.stop(0)
        }
    }
}
