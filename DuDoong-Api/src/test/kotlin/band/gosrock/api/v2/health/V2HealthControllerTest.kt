package band.gosrock.api.v2.health

import band.gosrock.api.v2.common.swagger.V2SwaggerGroups
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.user.exception.UserNotFoundException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 공통 - 실제 시큐리티/응답 래핑/Swagger 그룹 통합 테스트")
class V2HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("GET /api/v2/health 는 인증 없이 접근 가능하고 공통 성공 포맷으로 래핑된다")
    fun healthIsPublicAndWrapped() {
        mockMvc.get("/api/v2/health").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.status") { value(200) }
            jsonPath("$.data.status") { value("UP") }
            jsonPath("$.timeStamp") { exists() }
        }
    }

    @Test
    @DisplayName("공개 목록에 없는 v2 경로는 인증 없이 접근하면 401")
    fun nonPublicV2PathRequiresAuth() {
        mockMvc.get("/api/v2/not-public").andExpect {
            status { isUnauthorized() }
            jsonPath("$.success") { value(false) }
        }
    }

    // api-docs 는 기존 시큐리티 설정상 USER 권한이 필요하다. username 은 userId(Long)로 파싱되므로 숫자.
    @Test
    @WithMockUser(username = "1", roles = ["USER"])
    @DisplayName("Swagger v2-전체 그룹 문서에 /api/v2/health 가 있고 v2 경로만 포함한다 (#731 그룹 분리)")
    fun swaggerV2Group() {
        val paths = apiDocsPaths(V2SwaggerGroups.ALL)

        assertTrue(paths.contains("/api/v2/health"), "v2 문서에 /api/v2/health 가 없음: $paths")
        assertTrue(paths.all { it.startsWith("/api/v2/") }, "v2 문서에 v2 외 경로 포함: $paths")
    }

    @Test
    @WithMockUser(username = "1", roles = ["USER"])
    @DisplayName("Swagger v1 그룹 문서에 /api/v2 경로가 없고, 기존 에러 예시가 그룹 문서에도 붙는다")
    fun swaggerV1Group() {
        val docs = apiDocs("v1")
        val paths = docs.path("paths").fieldNames().asSequence().toList()

        assertTrue(paths.isNotEmpty())
        assertTrue(paths.all { it.startsWith("/api/v1/") }, "v1 문서에 v1 외 경로 포함: $paths")

        // ExampleController GET /api/v1/examples : @ApiErrorExceptionsExample(ExampleExceptionDocs) 의 '유저없을때'
        val status = UserNotFoundException.EXCEPTION.getErrorReason().status.toString()
        val example = docs.at("/paths/~1api~1v1~1examples/get/responses/$status/content/application~1json/examples/유저없을때")
        assertTrue(!example.isMissingNode, "v1 그룹 문서에 에러 예시가 적용되지 않음")
        assertEquals("USER_404_1", example.path("value").path("code").asText())
    }

    @Test
    @WithMockUser(username = "1", roles = ["USER"])
    @DisplayName("Swagger internal 그룹 문서는 /internal-api 경로만 포함한다")
    fun swaggerInternalGroup() {
        val paths = apiDocsPaths("internal")

        assertTrue(paths.isNotEmpty())
        assertTrue(paths.all { it.startsWith("/internal-api/") }, "internal 문서에 다른 경로 포함: $paths")
    }

    private fun apiDocs(group: String): JsonNode {
        val body = mockMvc.get("/v3/api-docs/{group}", group)
            .andExpect { status { isOk() } }
            .andReturn().response.getContentAsString(Charsets.UTF_8)
        return objectMapper.readTree(body)
    }

    private fun apiDocsPaths(group: String): List<String> =
        apiDocs(group).path("paths").fieldNames().asSequence().toList()
}
