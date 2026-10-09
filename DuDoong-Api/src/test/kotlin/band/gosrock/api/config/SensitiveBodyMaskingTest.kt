package band.gosrock.api.config

import band.gosrock.api.slack.sender.SlackInternalErrorSender
import band.gosrock.api.slack.sender.SlackThrottleErrorSender
import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper

/** 요청 본문 로그·Slack 오류 알림의 개인정보 마스킹 (#718 리뷰) */
@DisplayName("요청 본문 민감 정보 마스킹")
class SensitiveBodyMaskingTest {

    private val body = """{"eventId":1,"depositorName":"홍길동","refundAccount":{"bankName":"국민은행","accountHolder":"김철수","accountNumber":"123-45-678901"},""" +
        """"account":{"bankName":"신한은행","accountHolder":"고스락","accountNumber":"110-123-456789"},"contacts":[{"type":"PHONE","value":"010-9876-5432"}],""" +
        """"phoneNumber":"010-1111-2222","email":"me@test.com","contactValue":"x@y.z","quantity":2,"number":7}"""

    private val secrets = listOf("홍길동", "국민은행", "김철수", "123-45-678901", "신한은행", "110-123-456789", "010-9876-5432", "010-1111-2222", "me@test.com", "x@y.z")

    @Test
    fun `민감 키의 값만 가리고 나머지(키·다른 값)는 그대로`() {
        val masked = SensitiveBodyMasker.mask(body)!!
        secrets.forEach { assertFalse(masked.contains(it), "$it 노출: $masked") }
        assertTrue(masked.contains("\"accountNumber\":\"***\""))
        assertTrue(masked.contains("\"quantity\":2") && masked.contains("\"eventId\":1") && masked.contains("\"type\":\"PHONE\""))
        // 계좌와 무관한 number 키는 가리지 않는다 (#755 — 옛 v2 티켓 계좌 키 bank·holder·number 를 뺐다)
        assertTrue(masked.contains("\"number\":7"), masked)
        assertEquals(ObjectMapper().readTree(masked).at("/refundAccount/accountNumber").asText(), "***")
    }

    @Test
    fun `선물 링크 토큰은 경로·URL 에서 가리고, 선물 메모는 본문에서 가린다 (#719)`() {
        val token = "AbC-_123xyzAbC-_123xyzAbC-_123xyzAbC-_123x"
        assertEquals("/api/v2/gifts/***", SensitiveBodyMasker.maskPath("/api/v2/gifts/$token"))
        assertEquals("/api/v2/gifts/***/accept", SensitiveBodyMasker.maskPath("/api/v2/gifts/$token/accept"))
        assertEquals("http://localhost/api/v2/gifts/***/reject?x=1", SensitiveBodyMasker.maskPath("http://localhost/api/v2/gifts/$token/reject?x=1"))
        // 선물 id 경로(G-2·G-8)와 그 밖 경로는 그대로
        assertEquals("/api/v2/me/gifts/12", SensitiveBodyMasker.maskPath("/api/v2/me/gifts/12"))
        assertEquals("/api/v2/me/tickets/abc/gift", SensitiveBodyMasker.maskPath("/api/v2/me/tickets/abc/gift"))
        assertEquals("""{"memo":"***"}""", SensitiveBodyMasker.mask("""{"memo":"엄마"}"""))
    }

    @Test
    fun `MdcFilter 요청 로그에 선물 토큰이 남지 않는다 (#719)`() {
        val logger = LoggerFactory.getLogger(MdcFilter::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        try {
            val request = MockHttpServletRequest("POST", "/api/v2/gifts/SECRET_TOKEN_719/accept")
            MdcFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())
            val lines = appender.list.map { it.formattedMessage }
            assertTrue(lines.any { it.contains("/api/v2/gifts/***/accept") }, lines.toString())
            assertFalse(lines.any { it.contains("SECRET_TOKEN_719") }, lines.toString())
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `잘린 본문·숫자 값·대소문자·공백 있는 JSON 도 가린다`() {
        // 로그 상한에서 잘린 값도 가린다 (닫는 따옴표를 붙여 준다)
        assertEquals("{\"accountNumber\":\"***\"", SensitiveBodyMasker.mask("""{"accountNumber":"123-45-6"""))
        assertEquals("""{"PhoneNumber" : "***"}""", SensitiveBodyMasker.mask("""{"PhoneNumber" : 01012345678}"""))
        assertEquals("""{"email":"***","name":"a"}""", SensitiveBodyMasker.mask("""{"email":"a\"b@c","name":"a"}"""))
        assertEquals("plain text", SensitiveBodyMasker.mask("plain text"))
    }

    @Test
    fun `MdcFilter 요청 로그에 계좌번호·입금자명·연락처가 남지 않는다`() {
        val logger = LoggerFactory.getLogger(MdcFilter::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        try {
            val request = MockHttpServletRequest("POST", "/api/v2/me/orders/abc/cancel").apply {
                contentType = "application/json"
                setContent(body.toByteArray(Charsets.UTF_8))
            }
            // 컨트롤러처럼 본문을 읽는 체인
            val chain = MockFilterChain(object : jakarta.servlet.http.HttpServlet() {
                override fun service(req: jakarta.servlet.ServletRequest, res: jakarta.servlet.ServletResponse) {
                    req.inputStream.readAllBytes()
                }
            })
            MdcFilter().doFilter(request, MockHttpServletResponse(), chain)
            val logged = appender.list.joinToString("\n") { it.formattedMessage }
            assertTrue(logged.contains("POST /api/v2/me/orders/abc/cancel"), logged)
            assertTrue(logged.contains("\"quantity\":2"), logged)
            secrets.forEach { assertFalse(logged.contains(it), "$it 로그 노출: $logged") }
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `Slack 500 알림 본문에 계좌번호·입금자명·연락처가 없다`() {
        val provider = mock(SlackErrorNotificationProvider::class.java)
        val request = ContentCachingRequestWrapper(
            MockHttpServletRequest("POST", "/api/v2/me/orders").apply {
                contentType = "application/json"
                setContent(body.toByteArray(Charsets.UTF_8))
            },
        )
        request.inputStream.readAllBytes()
        SlackInternalErrorSender(ObjectMapper(), provider).execute(request, RuntimeException("boom"), 1L)

        // Kotlin 비-null 파라미터라 ArgumentCaptor 대신 기록된 호출에서 인자를 꺼낸다
        val call = mockingDetails(provider).invocations.single { it.method.name == "sendNotification" }
        val sent = (call.arguments[0] as List<*>).joinToString("\n") { it.toString() }
        assertTrue(sent.contains("quantity"), sent)
        secrets.forEach { assertFalse(sent.contains(it), "$it Slack 노출: $sent") }
    }

    private fun slackSent(provider: SlackErrorNotificationProvider): String {
        val call = mockingDetails(provider).invocations.single { it.method.name == "sendNotification" }
        return (call.arguments[0] as List<*>).joinToString("\n") { it.toString() }
    }

    private fun giftRequest(): ContentCachingRequestWrapper = ContentCachingRequestWrapper(
        MockHttpServletRequest("POST", "/api/v2/gifts/SECRET_TOKEN_719/accept").apply {
            contentType = "application/json"
            setContent(body.toByteArray(Charsets.UTF_8))
        },
    ).also { it.inputStream.readAllBytes() }

    @Test
    fun `Slack 500·rate limit 알림 URL 에 선물 토큰이 없고, rate limit 본문도 가린다 (#719)`() {
        val internal = mock(SlackErrorNotificationProvider::class.java)
        SlackInternalErrorSender(ObjectMapper(), internal).execute(giftRequest(), RuntimeException("boom"), 1L)
        val throttle = mock(SlackErrorNotificationProvider::class.java)
        SlackThrottleErrorSender(ObjectMapper(), throttle).execute(giftRequest(), 1L)
        for (sent in listOf(slackSent(internal), slackSent(throttle))) {
            assertTrue(sent.contains("/api/v2/gifts/***/accept"), sent)
            assertFalse(sent.contains("SECRET_TOKEN_719"), sent)
            secrets.forEach { assertFalse(sent.contains(it), "$it Slack 노출: $sent") }
        }
    }

    @Test
    fun `카카오 accessToken 본문 값도 가린다 (#763 토큰 본문 전달)`() {
        val masked = SensitiveBodyMasker.mask("""{"accessToken":"kakao.at.1","refreshToken":"rt.1","idToken":"id.1"}""")!!
        listOf("kakao.at.1", "rt.1", "id.1").forEach { assertFalse(masked.contains(it), "$it 노출: $masked") }
    }
}
