package band.gosrock.api.v2.health

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 공통 - 실제 시큐리티/응답 래핑 통합 테스트")
class V2HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var groupedOpenApis: List<GroupedOpenApi>

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

    @Test
    @DisplayName("Swagger 는 v1 / v2 / internal 그룹으로 분리된다")
    fun swaggerGroups() {
        val pathsByGroup = groupedOpenApis.associate { it.group to it.pathsToMatch }

        assertEquals(
            mapOf(
                "v1" to listOf("/api/v1/**"),
                "v2" to listOf("/api/v2/**"),
                "internal" to listOf("/internal-api/**"),
            ),
            pathsByGroup,
        )
    }
}
