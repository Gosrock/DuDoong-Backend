package band.gosrock.api.config

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.host.model.dto.request.UpdateHostSlackRequest
import band.gosrock.api.slack.sender.SlackThrottleErrorSender
import band.gosrock.common.properties.JwtProperties
import band.gosrock.common.properties.TossPaymentsProperties
import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.LoggingEvent
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
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
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
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
    @DisplayName("비밀값 기동 검사")
    inner class Secrets {
        private val okJwt = "0123456789abcdef0123456789abcdef0123"

        private fun validate(jwt: String, tossKey: String = "live_sk_x", mid: String = "mid", vararg profiles: String) {
            val env = MockEnvironment().apply { setActiveProfiles(*profiles) }
            RequiredSecretsValidator(env, JwtProperties(jwt, 3600, 3600), TossPaymentsProperties(tossKey, mid)).afterPropertiesSet()
        }

        @Test
        fun `값이 없으면(자리표시자 그대로) 기동 실패`() {
            assertThrows(IllegalStateException::class.java) { validate("\${JWT_SECRET_KEY}") }
            assertThrows(IllegalStateException::class.java) { validate("") }
            assertThrows(IllegalStateException::class.java) { validate(okJwt, tossKey = "\${TOSS_PAYMENTS_KEY}") }
            assertThrows(IllegalStateException::class.java) { validate(okJwt, mid = " ") }
        }

        @Test
        fun `JWT 키는 32바이트 이상`() {
            assertThrows(IllegalStateException::class.java) { validate("a".repeat(31)) }
            assertDoesNotThrow { validate("a".repeat(32)) }
        }

        @Test
        fun `운영·스테이징은 테스트용 JWT 키를 거부하고, 그 밖 프로필은 허용`() {
            val testKey = "testkeytestkeytestkeytestkeytestkeytestkeytestkeytestkeytestkey"
            assertThrows(IllegalStateException::class.java) { validate(testKey, profiles = arrayOf("prod")) }
            assertThrows(IllegalStateException::class.java) { validate(testKey, profiles = arrayOf("staging", "infrastructure")) }
            assertDoesNotThrow { validate(testKey, profiles = arrayOf("local", "common-local")) }
            assertDoesNotThrow { validate(okJwt, profiles = arrayOf("prod")) }
        }

        @Test
        fun `스프링 컨텍스트에서 값이 없으면 기동이 실패하고, 있으면 뜬다`() {
            // 테스트 JVM 에는 build.gradle.kts 가 JWT_SECRET_KEY 등을 넣으므로, 없는 상황은 다른 이름의 자리표시자로 만든다
            val runner = ApplicationContextRunner()
                .withUserConfiguration(SecretPropertiesConfig::class.java, RequiredSecretsValidator::class.java)
                .withPropertyValues("auth.jwt.access-exp=3600", "auth.jwt.refresh-exp=3600")
            runner.withPropertyValues("auth.jwt.secret-key=\${MISSING_JWT_764}", "toss.secret-key=\${MISSING_TOSS_764}", "toss.mid=\${MISSING_MID_764}")
                .run { context ->
                    assertTrue(context.startupFailure != null)
                    val message = generateSequence(context.startupFailure) { it.cause }.joinToString(" | ") { it.message.orEmpty() }
                    assertTrue(message.contains("JWT_SECRET_KEY 가 없습니다") && message.contains("TOSS_PAYMENTS_KEY 가 없습니다"), message)
                }
            runner.withPropertyValues("auth.jwt.secret-key=$okJwt", "toss.secret-key=live_sk_x", "toss.mid=mid")
                .run { context -> assertTrue(context.startupFailure == null, context.startupFailure?.toString()) }
        }

        @Test
        fun `공통 yml 에는 JWT·토스·AWS 키 기본값이 없다`() {
            val common = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application-common.yml")) }.`object`!!
            assertEquals("\${JWT_SECRET_KEY}", common.getProperty("auth.jwt.secret-key"))
            assertEquals("\${TOSS_PAYMENTS_KEY}", common.getProperty("toss.secret-key"))
            assertEquals("\${TOSS_MID}", common.getProperty("toss.mid"))
            val infra = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application-infrastructure.yml")) }.`object`!!
            assertEquals("\${AWS_ACCESS_KEY}", infra.getProperty("aws.access-key"))
            assertEquals("\${AWS_SECRET_KEY}", infra.getProperty("aws.secret-key"))
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

    @EnableConfigurationProperties(JwtProperties::class, TossPaymentsProperties::class)
    class SecretPropertiesConfig
}
