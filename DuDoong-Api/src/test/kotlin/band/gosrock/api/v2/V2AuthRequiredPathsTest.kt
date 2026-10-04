package band.gosrock.api.v2

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * 비로그인 401 회귀 (#716 M-2 → #721 자동 열거).
 *
 * `RequestMappingHandlerMapping` 에 등록된 `/api/v2/` 매핑을 전부 모아, 아래 [PUBLIC_GET] 밖의 모든 (메서드, 경로)를 비로그인으로 호출해 401 을 단언한다.
 * `SecurityConfig.V2_PUBLIC_GET_PATHS` 를 실수로 넓히면(예: `/api/v2/events/` 하위 전체 와일드카드) 같은 접두의 호스트 전용 GET 이 401 이 아니게 되어 잡히고,
 * 새 컨트롤러 경로는 목록 수정 없이 자동으로 검사된다. 공개 경로를 정말 추가할 때만 [PUBLIC_GET] 을 함께 고친다 (기대값을 SecurityConfig 에서 읽지 않는다 — 독립 기대값).
 *
 * 경로 변수는 필터 단계에서 막히므로 아무 값이나 되지만, 공개 패턴의 정규식(`[0-9]+`)과 같은 모양이 되도록 숫자(이름에 uuid 가 있으면 UUID)로 바꾼다.
 * 그래야 "숫자 id 면 열리는" 공개 패턴이 다른 메서드·하위 경로를 여는지까지 본다.
 *
 * 판단 기록 (#721): 기존 수동 목록(20건)은 이 자동 열거로 대체했다. 열거 자체가 비는 회귀(매핑 수집 실패)를 막기 위해 예전 목록의 핵심 경로는
 * [MUST_BE_ENUMERATED] 로 '열거에 포함되는지'만 확인한다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 인증 필요 경로 - 비로그인 401 (매핑 자동 열거)")
class V2AuthRequiredPathsTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private data class Endpoint(val method: HttpMethod, val pattern: String) {
        val normalized: String get() = normalize(pattern)
        override fun toString() = "${method.name()} $pattern"
    }

    private fun v2Endpoints(): List<Endpoint> =
        handlerMapping.handlerMethods.keys.flatMap { info ->
            val patterns = info.pathPatternsCondition?.patternValues ?: info.patternsCondition?.patterns.orEmpty()
            val methods = info.methodsCondition.methods.ifEmpty { ALL_METHODS }
            patterns.filter { it.startsWith("/api/v2/") }.flatMap { p -> methods.map { Endpoint(HttpMethod.valueOf(it.name), p) } }
        }.distinct().sortedBy { it.toString() }

    private fun isPublic(endpoint: Endpoint) = endpoint.method == HttpMethod.GET && endpoint.normalized in PUBLIC_GET

    private fun anonymousStatus(endpoint: Endpoint): Int =
        mockMvc.perform(
            MockMvcRequestBuilders.request(endpoint.method, concretePath(endpoint.pattern))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andReturn().response.status

    @TestFactory
    fun `공개 GET 밖의 모든 v2 매핑은 비로그인 401`(): List<DynamicTest> =
        v2Endpoints().filterNot { isPublic(it) }.map { endpoint ->
            DynamicTest.dynamicTest(endpoint.toString()) {
                assertEquals(401, anonymousStatus(endpoint), "$endpoint (${concretePath(endpoint.pattern)})")
            }
        }

    @Test
    fun `공개 GET 목록은 모두 실제 매핑이 있고 비로그인으로 401 이 아니다`() {
        val publicEndpoints = v2Endpoints().filter { isPublic(it) }
        assertEquals(PUBLIC_GET, publicEndpoints.map { it.normalized }.toSet(), "PUBLIC_GET 중 매핑이 없는 경로가 있음 (삭제·이름 변경 시 목록 갱신)")
        publicEndpoints.forEach { assertNotEquals(401, anonymousStatus(it), "$it 는 공개 경로") }
    }

    @Test
    fun `매핑 열거에 기존 인증 필요 경로가 포함된다 (열거 실패 방지)`() {
        val enumerated = v2Endpoints().map { "${it.method.name()} ${it.normalized}" }.toSet()
        val missing = MUST_BE_ENUMERATED - enumerated
        assertTrue(missing.isEmpty(), "열거에 없음: $missing")
    }

    companion object {
        private val ALL_METHODS = setOf(RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE)

        private val PATH_VARIABLE = Regex("\\{([^}:]+)(:[^}]*)?}")

        /** `{name:regex}` / `{name}` → `{}` (컨트롤러와 SecurityConfig 의 변수 이름·정규식 차이를 무시) */
        private fun normalize(pattern: String) = pattern.replace(PATH_VARIABLE, "{}")

        private val DUMMY_UUID = UUID(0L, 1L).toString()

        private fun concretePath(pattern: String) =
            pattern.replace(PATH_VARIABLE) { if (it.groupValues[1].contains("uuid", ignoreCase = true)) DUMMY_UUID else "1" }

        /** 비로그인 공개 GET (SecurityConfig.V2_PUBLIC_GET_PATHS 와 같아야 함, 경로 변수는 `{}`) */
        private val PUBLIC_GET = setOf(
            "/api/v2/health",
            "/api/v2/hosts/{}",
            "/api/v2/hosts/{}/events",
            "/api/v2/tags",
            "/api/v2/events/{}/sections",
            "/api/v2/home",
            "/api/v2/events",
            "/api/v2/events/{}",
            "/api/v2/events/{}/ticket-items",
        )

        /** #716·#718 수동 목록에서 옮긴 핵심 인증 필요 경로 */
        private val MUST_BE_ENUMERATED = setOf(
            "GET /api/v2/events/{}/manage",
            "GET /api/v2/events/{}/checklist",
            "GET /api/v2/events/{}/check-in-qr",
            "GET /api/v2/events/{}/check-ins/stats",
            "GET /api/v2/events/{}/dashboard",
            "GET /api/v2/events/{}/orders",
            "GET /api/v2/events/{}/orders/export",
            "GET /api/v2/events/{}/issued-tickets",
            "GET /api/v2/events/{}/options",
            "GET /api/v2/events/{}/ticket-items/manage",
            "GET /api/v2/me/events",
            "DELETE /api/v2/events/{}",
            "PATCH /api/v2/events/{}/basic",
            "POST /api/v2/events/{}/open",
            "POST /api/v2/events/{}/ticket-items",
            "POST /api/v2/events",
            "POST /api/v2/orders",
            "GET /api/v2/me/orders",
            "GET /api/v2/me/orders/{}",
            "POST /api/v2/me/orders/{}/cancel",
        )
    }
}
