package band.gosrock.api.v2.common

import band.gosrock.api.config.response.GlobalExceptionHandler
import band.gosrock.api.config.response.SuccessResponseAdvice
import band.gosrock.api.slack.sender.SlackInternalErrorSender
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.ForbiddenHostException
import band.gosrock.domain.domains.host.exception.HostErrorCode
import band.gosrock.domain.domains.host.exception.HostNotFoundException
import band.gosrock.domain.domains.host.exception.NotAcceptedHostException
import band.gosrock.domain.domains.host.exception.NotManagerHostException
import band.gosrock.domain.domains.host.exception.NotMasterHostException
import band.gosrock.domain.domains.host.exception.NotPartnerHostException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@DisplayName("V2ErrorPolicy - 호스트 권한 실패 상태 코드 분기")
class V2ErrorPolicyTest {

    /**
     * 테스트 전용 핸들러. v1/v2 경로에서 같은 예외를 던진다.
     * inner class 는 독립 클래스가 아니므로 통합 테스트(@ComponentScan band.gosrock)의 스캔 대상에서 제외된다.
     */
    @RestController
    inner class HostErrorTestController {
        @GetMapping("/api/v1/test/host-errors/{name}", "/api/v2/test/host-errors/{name}")
        fun throwError(@PathVariable name: String): Unit = throw EXCEPTIONS.getValue(name)
    }

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(HostErrorTestController())
            .setControllerAdvice(
                GlobalExceptionHandler(mock(SlackInternalErrorSender::class.java)),
                SuccessResponseAdvice(),
            )
            .build()
    }

    @Nested
    @DisplayName("HTTP 응답")
    inner class HttpResponse {

        @ParameterizedTest(name = "{0}: v2 는 403")
        @ValueSource(strings = ["FORBIDDEN_HOST", "NOT_ACCEPTED_HOST", "NOT_MANAGER_HOST", "NOT_MASTER_HOST", "NOT_PARTNER_HOST"])
        fun v2ReturnsForbidden(name: String) {
            val errorCode = HostErrorCode.valueOf(name)

            mockMvc.get("/api/v2/test/host-errors/$name").andExpect {
                status { isForbidden() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.status") { value(403) }
                jsonPath("$.code") { value(errorCode.getErrorReason().code) }
                jsonPath("$.reason") { value(errorCode.getReason()) }
                jsonPath("$.path") { value("http://localhost/api/v2/test/host-errors/$name") }
                jsonPath("$.timeStamp") { exists() }
            }
        }

        @ParameterizedTest(name = "{0}: v1 은 기존 400 유지")
        @ValueSource(strings = ["FORBIDDEN_HOST", "NOT_ACCEPTED_HOST", "NOT_MANAGER_HOST", "NOT_MASTER_HOST", "NOT_PARTNER_HOST"])
        fun v1KeepsBadRequest(name: String) {
            val errorCode = HostErrorCode.valueOf(name)

            mockMvc.get("/api/v1/test/host-errors/$name").andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.status") { value(400) }
                jsonPath("$.code") { value(errorCode.getErrorReason().code) }
                jsonPath("$.reason") { value(errorCode.getReason()) }
            }
        }

        @Test
        @DisplayName("v2 라도 권한과 무관한 호스트 에러는 원래 상태 코드를 유지한다")
        fun v2KeepsNonPermissionErrors() {
            mockMvc.get("/api/v2/test/host-errors/ALREADY_JOINED_HOST").andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("HOST_400_3") }
            }
            mockMvc.get("/api/v2/test/host-errors/HOST_NOT_FOUND").andExpect {
                status { isNotFound() }
                jsonPath("$.status") { value(404) }
            }
        }
    }

    @Nested
    @DisplayName("resolve")
    inner class Resolve {

        @Test
        @DisplayName("/api/v2 로 시작하지 않는 경로는 원래 ErrorReason 을 그대로 반환한다")
        fun nonV2Path() {
            val code = HostErrorCode.FORBIDDEN_HOST

            assertEquals(code.getErrorReason(), V2ErrorPolicy.resolve("/api/v1/hosts/1", code))
            assertEquals(code.getErrorReason(), V2ErrorPolicy.resolve("/internal-api/v1/hosts", code))
            assertEquals(code.getErrorReason(), V2ErrorPolicy.resolve("/api/v2", code))
        }

        @Test
        @DisplayName("v2 경로의 권한 코드는 status 만 403 으로 바꾸고 code/reason 은 유지한다")
        fun v2Path() {
            val code = HostErrorCode.NOT_MANAGER_HOST

            val result = V2ErrorPolicy.resolve("/api/v2/hosts/1", code)

            assertEquals(code.getErrorReason().copy(status = 403), result)
        }
    }

    companion object {
        private val EXCEPTIONS: Map<String, DuDoongCodeException> = mapOf(
            "FORBIDDEN_HOST" to ForbiddenHostException.EXCEPTION,
            "NOT_ACCEPTED_HOST" to NotAcceptedHostException.EXCEPTION,
            "NOT_MANAGER_HOST" to NotManagerHostException.EXCEPTION,
            "NOT_MASTER_HOST" to NotMasterHostException.EXCEPTION,
            "NOT_PARTNER_HOST" to NotPartnerHostException.EXCEPTION,
            "ALREADY_JOINED_HOST" to AlreadyJoinedHostException.EXCEPTION,
            "HOST_NOT_FOUND" to HostNotFoundException.EXCEPTION,
        )
    }
}
