package band.gosrock.api.v2

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 비로그인 401 회귀 (#716 M-2). 공개 GET 경로(`/api/v2/events`, `/api/v2/events/{id}`, `/api/v2/events/{id}/ticket-items`)를
 * 추가하면서 같은 접두의 호스트 전용 경로·다른 메서드가 공개되지 않았는지 확인한다 (SecurityConfig.V2_PUBLIC_GET_PATHS).
 * 필터 단계에서 막히므로 공연 id 는 아무 값이나 쓴다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 인증 필요 경로 - 비로그인 401")
class V2AuthRequiredPathsTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(
        "GET, /api/v2/events/1/manage",
        "GET, /api/v2/events/1/checklist",
        "GET, /api/v2/events/1/check-in-qr",
        "GET, /api/v2/events/1/check-ins/stats",
        "GET, /api/v2/events/1/dashboard",
        "GET, /api/v2/events/1/orders",
        "GET, /api/v2/events/1/orders/export",
        "GET, /api/v2/events/1/issued-tickets",
        "GET, /api/v2/events/1/options",
        "GET, /api/v2/events/1/ticket-items/manage",
        "GET, /api/v2/me/events",
        "DELETE, /api/v2/events/1",
        "PATCH, /api/v2/events/1/basic",
        "POST, /api/v2/events/1/open",
        "POST, /api/v2/events/1/ticket-items",
        "POST, /api/v2/events",
    )
    fun `비로그인 요청은 401`(method: String, path: String) {
        mockMvc.perform(
            MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isUnauthorized)
    }
}
