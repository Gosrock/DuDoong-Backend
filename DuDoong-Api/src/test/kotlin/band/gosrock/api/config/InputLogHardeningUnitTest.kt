package band.gosrock.api.config

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.host.model.dto.request.UpdateHostSlackRequest
import band.gosrock.api.slack.sender.SlackThrottleErrorSender
import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.LoggingEvent
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.slf4j.MDC
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Profiles
import org.springframework.core.io.ClassPathResource
import org.springframework.core.task.TaskRejectedException
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper

@DisplayName("#764 입력·외부 호출·로그 보강 (단위)")
class InputLogHardeningUnitTest {

    @Nested
    @DisplayName("요청 제한 Slack 알림")
    inner class ThrottleAlert {

        private fun request(ip: String = "198.51.100.7") = ContentCachingRequestWrapper(
            MockHttpServletRequest("GET", "/api/v2/health").apply { remoteAddr = ip },
        )

        private fun sentCount(provider: SlackErrorNotificationProvider) =
            mockingDetails(provider).invocations.count { it.method.name == "sendNotification" }

        @Test
        fun `같은 키는 1분에 한 번만, 다른 키·1분 뒤에는 다시 보낸다`() {
            val provider = mock(SlackErrorNotificationProvider::class.java)
            val sender = SlackThrottleErrorSender(ObjectMapper(), provider)
            val t0 = 1_000_000L
            repeat(5) { sender.execute(request(), 0L, t0 + it * 1000) }
            assertEquals(1, sentCount(provider), "같은 IP 1분 안에는 1번")
            sender.execute(request("198.51.100.8"), 0L, t0)
            sender.execute(request(), 7L, t0)
            assertEquals(3, sentCount(provider), "다른 IP·유저는 따로")
            sender.execute(request(), 0L, t0 + SlackThrottleErrorSender.INTERVAL_MILLIS)
            assertEquals(4, sentCount(provider), "1분 지나면 다시")
        }

        @Test
        fun `비동기 풀이 가득 차 거절돼도 예외를 던지지 않는다`() {
            val provider = mock(SlackErrorNotificationProvider::class.java)
            doThrow(TaskRejectedException("full")).`when`(provider).sendNotification(anyList())
            assertDoesNotThrow { SlackThrottleErrorSender(ObjectMapper(), provider).execute(request(), 0L, 1L) }
        }
    }

    @Nested
    @DisplayName("입력 형식")
    inner class Inputs {
        private val validator = Validation.buildDefaultValidatorFactory().validator

        @Test
        fun `호스트 슬랙 URL 은 Slack Incoming Webhook 형식만`() {
            assertTrue(validator.validate(UpdateHostSlackRequest("https://hooks.slack.com/services/T0/B0/xyz")).isEmpty())
            listOf("https://slack.dd.com", "http://127.0.0.1/services/T0", "https://hooks.slack.com.example.com/services/T0")
                .forEach { assertFalse(validator.validate(UpdateHostSlackRequest(it)).isEmpty(), it) }
        }

        @Test
        fun `회원가입 프로필 이미지는 카카오 CDN 주소·빈 값만`() {
            fun valid(image: String?) = validator.validate(RegisterRequest(email = "a@b.c", name = "n", profileImage = image)).isEmpty()
            assertTrue(valid(null))
            assertTrue(valid(""))
            assertTrue(valid("http://k.kakaocdn.net/dn/abc/img_640x640.jpg"))
            assertTrue(valid("https://img1.kakaocdn.net/thumb/R640x640/abc.jpg"))
            assertFalse(valid("https://example.com/kakao.png"))
            assertFalse(valid("https://k.kakaocdn.net.example.com/a.jpg"))
            assertFalse(valid("https://example.com/k.kakaocdn.net/a.jpg"))
        }
    }

    @Nested
    @DisplayName("로그")
    inner class Logs {

        @Test
        fun `X-Trace-Id 는 영문·숫자·하이픈 64자 이하만 받고 아니면 새로 만든다`() {
            assertEquals("a1b2c3d4e5f60718293a4b5c6d7e8f90", MdcFilter.resolveTraceId("a1b2c3d4e5f60718293a4b5c6d7e8f90"))
            assertEquals("trace-1", MdcFilter.resolveTraceId("trace-1"))
            listOf(null, "", "a\nb", "a b", "x".repeat(65), "a\r\n[INFO] fake").forEach {
                val resolved = MdcFilter.resolveTraceId(it)
                assertTrue(Regex("^[0-9a-f]{8}$").matches(resolved), "$it -> $resolved")
            }
        }

        @Test
        fun `MdcFilter 는 형식이 아닌 X-Trace-Id 를 응답 헤더·MDC 에 쓰지 않는다`() {
            val response = MockHttpServletResponse()
            var mdcTrace: String? = null
            val chain = MockFilterChain(object : jakarta.servlet.http.HttpServlet() {
                override fun service(req: jakarta.servlet.ServletRequest, res: jakarta.servlet.ServletResponse) {
                    mdcTrace = MDC.get("traceId")
                }
            })
            MdcFilter().doFilter(MockHttpServletRequest("GET", "/api/v2/health").apply { addHeader("X-Trace-Id", "bad id!") }, response, chain)
            assertEquals(mdcTrace, response.getHeader("X-Trace-Id"))
            assertTrue(Regex("^[0-9a-f]{8}$").matches(mdcTrace!!), mdcTrace)
        }

        @Test
        fun `콘솔 로그 패턴은 메시지 안의 개행을 공백으로 바꿔 한 줄로 남긴다`() {
            val yaml = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application.yml")) }.`object`!!
            val pattern = yaml.getProperty("logging.pattern.console")
            val context = org.slf4j.LoggerFactory.getILoggerFactory() as LoggerContext
            val layout = PatternLayout().apply {
                this.context = context
                this.pattern = pattern
                start()
            }
            val logger = context.getLogger("test")
            val line = layout.doLayout(LoggingEvent("x", logger, ch.qos.logback.classic.Level.INFO, "입력=a\r\n[ERROR] 가짜 줄\nb", null, null))
            assertEquals(1, line.trimEnd('\n', '\r').lines().size, line)
            assertTrue(line.contains("입력=a [ERROR] 가짜 줄 b"), line)
        }

        @Test
        fun `결제·인증 값과 연락처·계좌번호 계열 키를 가린다`() {
            val body = """{"paymentKey":"pk_live_1","refreshToken":"rt.1","idToken":"id.1","receiverPhone":"010-1","phone":"010-2",""" +
                """"guardianPhoneNo":"010-3","refundAccountNumber":"110-1","accountNo":"110-2","orderId":"R1","amount":1000}"""
            val masked = SensitiveBodyMasker.mask(body)!!
            listOf("pk_live_1", "rt.1", "id.1", "010-1", "010-2", "010-3", "110-1", "110-2").forEach { assertFalse(masked.contains(it), "$it 노출: $masked") }
            assertTrue(masked.contains("\"orderId\":\"R1\"") && masked.contains("\"amount\":1000"), masked)
        }
    }

    @Nested
    @DisplayName("운영 기본 비밀값 경고")
    inner class Secrets {
        private val jwtDefault = "testkeytestkeytestkeytestkeytestkeytestkeytestkeytestkeytestkey"
        private val values = mapOf(
            "auth.jwt.secret-key" to jwtDefault,
            "toss.secret-key" to "test_sk_ADpexMgkW36weAqp4bNVGbR5ozO0",
            "toss.mid" to "gosroc9mwo",
            "aws.access-key" to "testKey",
            "aws.secret-key" to "secretKey",
        )

        private fun warner(profile: String, overrides: Map<String, String> = emptyMap()): Pair<DefaultSecretsWarner, SlackErrorNotificationProvider> {
            val env = MockEnvironment().apply {
                setActiveProfiles(profile)
                (values + overrides).forEach { (k, v) -> setProperty(k, v) }
            }
            val slack = mock(SlackErrorNotificationProvider::class.java)
            return DefaultSecretsWarner(env, slack) to slack
        }

        @Test
        fun `공개 기본값과 같은 설정 이름만 고른다`() {
            assertEquals(values.keys.toList(), warner("prod").first.defaultSecretsInUse())
            assertEquals(listOf("toss.mid"), warner("prod", values.mapValues { "real-${it.key}" } - "toss.mid").first.defaultSecretsInUse())
        }

        @Test
        fun `JWT 키가 기본값이 아니어도 testkey 로 시작하면 고른다`() {
            val real = values.mapValues { "real-${it.key}" }
            assertEquals(listOf("auth.jwt.secret-key"), warner("staging", real + ("auth.jwt.secret-key" to "testkey-other-0123456789012345678901")).first.defaultSecretsInUse())
        }

        @Test
        fun `운영에서 기본값이면 ERROR 로그·Slack 알림에 이름만 남기고 값은 남기지 않는다 (기동은 계속)`() {
            val logger = org.slf4j.LoggerFactory.getLogger(DefaultSecretsWarner::class.java) as ch.qos.logback.classic.Logger
            val appender = ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>().also { it.start() }
            logger.addAppender(appender)
            try {
                val (w, slack) = warner("prod")
                assertDoesNotThrow { w.warnIfDefaultSecrets() }
                val errors = appender.list.filter { it.level == ch.qos.logback.classic.Level.ERROR }.map { it.formattedMessage }
                assertEquals(1, errors.size, errors.toString())
                assertTrue(errors[0].contains("auth.jwt.secret-key") && errors[0].contains("aws.secret-key"), errors[0])
                val sent = mockingDetails(slack).invocations.filter { it.method.name == "sendNotification" }.joinToString { it.arguments[0].toString() }
                assertTrue(sent.contains("auth.jwt.secret-key"), sent)
                (values.values + errors).forEach { _ -> }
                values.values.forEach { v -> assertFalse(errors[0].contains(v) || sent.contains(v), "값 노출: $v") }
            } finally {
                logger.detachAppender(appender)
            }
        }

        @Test
        fun `스테이징도 같은 경고를 남긴다`() {
            val (w, slack) = warner("staging")
            assertDoesNotThrow { w.warnIfDefaultSecrets() }
            assertEquals(1, mockingDetails(slack).invocations.count { it.method.name == "sendNotification" })
        }

        @Test
        fun `운영·스테이징이 아니거나 실제 값이면 아무것도 하지 않는다`() {
            listOf(warner("local"), warner("dev"), warner("prod", values.mapValues { "real-${it.key}" })).forEach { (w, slack) ->
                w.warnIfDefaultSecrets()
                assertEquals(0, mockingDetails(slack).invocations.size)
            }
        }
    }

    @Test
    fun `Swagger 설정은 스테이징·운영 둘 다 아닐 때만 켜진다`() {
        val expression = SwaggerConfig::class.java.getAnnotation(Profile::class.java).value
        fun active(vararg profiles: String) = MockEnvironment().apply { setActiveProfiles(*profiles) }.acceptsProfiles(Profiles.of(*expression))
        assertFalse(active("prod"))
        assertFalse(active("staging"))
        assertTrue(active("local"))
        assertTrue(active("dev"))
    }
}
