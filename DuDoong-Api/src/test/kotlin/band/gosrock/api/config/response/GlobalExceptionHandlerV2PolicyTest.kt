package band.gosrock.api.config.response

import band.gosrock.api.slack.sender.SlackInternalErrorSender
import band.gosrock.api.v2.support.HOST_ERROR_EXCEPTIONS
import band.gosrock.api.v2.support.V2TestHandlers
import band.gosrock.domain.domains.host.exception.HostErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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

@DisplayName("GlobalExceptionHandler - v2 핸들러 호스트 권한 실패 403 / v1 400")
class GlobalExceptionHandlerV2PolicyTest {

    /**
     * v2 패키지 밖(v1)의 테스트 전용 컨트롤러. 경로가 /api/v2 여도 패키지 기준으로 v1 정책(400)이 적용되는지 확인한다.
     * inner class 라 통합 테스트의 컴포넌트 스캔 대상에서 제외된다.
     */
    @RestController
    inner class V1HostErrorController {
        @GetMapping("/api/v1/test/host-errors/{name}", "/api/v2/test/v1-handler/host-errors/{name}")
        fun throwError(@PathVariable name: String): Unit = throw HOST_ERROR_EXCEPTIONS.getValue(name)
    }

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(V1HostErrorController(), V2TestHandlers().HostErrorController())
            .setControllerAdvice(
                GlobalExceptionHandler(mock(SlackInternalErrorSender::class.java)),
                SuccessResponseAdvice(),
            )
            .build()
    }

    @ParameterizedTest(name = "{0}: v2 핸들러는 403")
    @ValueSource(strings = ["FORBIDDEN_HOST", "NOT_ACCEPTED_HOST", "NOT_MANAGER_HOST", "NOT_MASTER_HOST", "NOT_PARTNER_HOST"])
    fun v2HandlerReturnsForbidden(name: String) {
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

    @ParameterizedTest(name = "{0}: v1 핸들러는 기존 400 유지")
    @ValueSource(strings = ["FORBIDDEN_HOST", "NOT_ACCEPTED_HOST", "NOT_MANAGER_HOST", "NOT_MASTER_HOST", "NOT_PARTNER_HOST"])
    fun v1HandlerKeepsBadRequest(name: String) {
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
    @DisplayName("판정은 경로가 아니라 핸들러 패키지 기준이다")
    fun decidedByHandlerPackageNotPath() {
        // v1 핸들러가 /api/v2 경로를 받아도 400
        mockMvc.get("/api/v2/test/v1-handler/host-errors/FORBIDDEN_HOST").andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
        }
        // v2 핸들러는 경로가 /api/v2 가 아니어도 403
        mockMvc.get("/test/v2-handler/host-errors/FORBIDDEN_HOST").andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    @DisplayName("v2 핸들러라도 권한과 무관한 호스트 에러는 원래 상태 코드를 유지한다")
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
