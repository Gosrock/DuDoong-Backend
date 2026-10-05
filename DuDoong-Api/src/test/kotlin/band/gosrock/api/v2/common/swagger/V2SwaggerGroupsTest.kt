package band.gosrock.api.v2.common.swagger

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * v2 Swagger 그룹·태그 (#731). 실제 `/v3/api-docs/{그룹}` JSON 과 `RequestMappingHandlerMapping` 의 `/api/v2/` 매핑을 자동 열거해 비교한다 —
 * 새 컨트롤러도 목록 수정 없이 검사된다 (영역 분류가 안 되거나 태그 규칙을 어기면 실패). Swagger 문서도 로그인(USER)이 필요하다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 Swagger 그룹·태그")
class V2SwaggerGroupsTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private data class Op(val method: String, val path: String, val tags: List<String>, val summary: String?) {
        val key get() = "$method $path"
    }

    private fun docs(group: String): JsonNode =
        objectMapper.readTree(
            mockMvc.get("/v3/api-docs/{group}", group) { with(user("1").roles("USER")) }.andExpect { status { isOk() } }.andReturn().response.getContentAsString(Charsets.UTF_8),
        )

    private fun ops(group: String): List<Op> {
        val docs = docs(group)
        return docs["paths"].fields().asSequence().flatMap { (path, item) ->
            item.fields().asSequence().map { (method, op) ->
                Op(method.uppercase(), path, op["tags"]?.map { it.asText() }.orEmpty(), op["summary"]?.asText())
            }
        }.toList()
    }

    /** 핸들러 매핑의 v2 (메서드 경로) → 컨트롤러 클래스 */
    private fun v2Handlers(): Map<String, Class<*>> =
        handlerMapping.handlerMethods.flatMap { (info, handler) ->
            val patterns = info.pathPatternsCondition?.patternValues ?: info.patternsCondition?.patterns.orEmpty()
            patterns.filter { it.startsWith("/api/v2/") }.flatMap { p -> info.methodsCondition.methods.map { "${it.name} $p" to handler.beanType } }
        }.toMap()

    @Test
    fun `모든 v2 컨트롤러는 영역이 정해진다 (패키지 기본값 또는 @V2Area)`() {
        val unclassified = v2Handlers().filterValues { V2ApiArea.of(it) == null }
        assertTrue(unclassified.isEmpty(), "영역 미분류: $unclassified — V2ApiArea.PACKAGE_AREAS 또는 @V2Area 추가")
    }

    @Test
    fun `v2 경로는 영역 그룹 중 정확히 하나에 들어가고, 합치면 v2-전체·핸들러 매핑과 같다`() {
        val all = ops(V2SwaggerGroups.ALL).map { it.key }.toSet()
        val hosting = ops(V2ApiArea.HOSTING.group).map { it.key }.toSet()
        val user = ops(V2ApiArea.USER.group).map { it.key }.toSet()
        assertEquals(v2Handlers().keys, all, "v2-전체 문서 = 핸들러 매핑")
        assertEquals(emptySet<String>(), hosting intersect user, "두 영역에 모두 있음")
        assertEquals(all, hosting + user, "어느 영역에도 없음: ${all - hosting - user}")
        // 영역 분류 기준과 그룹 내용이 같다
        v2Handlers().forEach { (key, controller) ->
            val expected = if (V2ApiArea.of(controller) == V2ApiArea.HOSTING) hosting else user
            assertTrue(key in expected, "$key ($controller) 가 ${V2ApiArea.of(controller)} 그룹에 없음")
        }
        // 같은 경로라도 영역이 다르면 나뉜다 (P-2 공연 리스트 / E-2 공연 생성)
        assertTrue("GET /api/v2/events" in user && "POST /api/v2/events" in hosting)
    }

    @Test
    fun `v2 태그는 하나씩, '(영역) 번호 이름' 형식이고 영역이 그룹과 맞으며 v1 태그가 섞이지 않는다`() {
        for (area in V2ApiArea.entries) {
            ops(area.group).forEach { op ->
                assertEquals(1, op.tags.size, "${op.key} 태그 ${op.tags}")
                val tag = op.tags.single()
                val match = TAG_FORMAT.matchEntire(tag)
                assertNotNull(match, "${op.key} 태그 형식: $tag")
                assertEquals(area.tagPrefix, "[${match!!.groupValues[1]}]", "${op.key} 의 태그 $tag 가 ${area.group} 과 다른 영역")
            }
        }
        val docs = docs(V2SwaggerGroups.ALL)
        val tagNames = docs["tags"].map { it["name"].asText() }
        assertTrue(tagNames.all { TAG_FORMAT.matches(it) }, "v2-전체 문서의 태그에 v1·형식 밖 태그: ${tagNames.filterNot { TAG_FORMAT.matches(it) }}")
        assertTrue(tagNames.all { docs["tags"].first { t -> t["name"].asText() == it }["description"]?.asText().orEmpty().isNotBlank() }, "설명 없는 태그")
    }

    @Test
    fun `태그 번호는 영역 안에서 겹치지 않는다`() {
        val tags = ops(V2SwaggerGroups.ALL).flatMap { it.tags }.toSet()
        val byNumber = tags.groupBy { TAG_FORMAT.matchEntire(it)!!.let { m -> m.groupValues[1] to m.groupValues[2] } }
        val duplicated = byNumber.filterValues { it.size > 1 }
        assertTrue(duplicated.isEmpty(), "번호 중복: $duplicated")
    }

    @Test
    fun `operation summary 는 화면 ID 로 시작한다 (헬스체크 제외)`() {
        val missing = ops(V2SwaggerGroups.ALL).filter { it.path != "/api/v2/health" && !SCREEN_ID.containsMatchIn(it.summary.orEmpty()) }
        assertTrue(missing.isEmpty(), "화면 ID 없음: ${missing.map { "${it.key}: ${it.summary}" }}")
    }

    @Test
    fun `기본 그룹은 v2-전체, Swagger UI 정렬 설정 없음(문서 순서), v1·internal 그룹 유지, 그룹 설명에 권한 표기·에러 코드 안내`() {
        val config = objectMapper.readTree(mockMvc.get("/v3/api-docs/swagger-config") { with(user("1").roles("USER")) }.andExpect { status { isOk() } }.andReturn().response.getContentAsString(Charsets.UTF_8))
        assertEquals("v2-전체", config["urls.primaryName"].asText())
        // 전역 정렬을 켜면 아래 문서 순서(태그: 호스팅 → 사용자, operation: 화면 ID)가 무시된다
        assertTrue(config["tagsSorter"] == null && config["operationsSorter"] == null, config.toString())
        assertEquals(setOf("v1", "internal", "v2-전체", "v2-호스팅센터", "v2-사용자앱"), config["urls"].map { it["name"].asText() }.toSet())
        for (group in listOf(V2SwaggerGroups.ALL, V2ApiArea.HOSTING.group, V2ApiArea.USER.group)) {
            val description = docs(group)["info"]["description"].asText()
            listOf("G+", "M+", "MS", "에러 코드").forEach { assertTrue(it in description, "$group 설명에 $it 없음") }
        }
    }

    @Test
    fun `태그는 호스팅 1~8 → 사용자 1~4 순서 (tags 배열), 문서에 쓰인 태그는 모두 순서 목록에 있다`() {
        for (group in listOf(V2SwaggerGroups.ALL, V2ApiArea.HOSTING.group, V2ApiArea.USER.group)) {
            val docs = docs(group)
            val tags = docs["tags"].map { it["name"].asText() }
            assertEquals(V2ApiTags.ORDERED.filter { it in tags }, tags, "$group 태그 순서")
            val used = ops(group).flatMap { it.tags }.toSet()
            assertEquals(used, tags.toSet(), "$group: tags 배열 = 쓰인 태그")
            assertTrue((used - V2ApiTags.ORDERED.toSet()).isEmpty(), "V2ApiTags.ORDERED 에 없는 태그: ${used - V2ApiTags.ORDERED.toSet()}")
        }
        val all = docs(V2SwaggerGroups.ALL)["tags"].map { it["name"].asText() }
        assertEquals(all.sortedBy { if (it.startsWith(V2ApiArea.HOSTING.tagPrefix)) 0 else 1 }, all, "호스팅이 사용자보다 먼저")
    }

    @Test
    fun `태그 안 operation 은 화면 ID 순 (경로 단위 — 경로 순서 = 그 경로 화면 ID 중 가장 앞)`() {
        for (group in listOf(V2SwaggerGroups.ALL, V2ApiArea.HOSTING.group, V2ApiArea.USER.group)) {
            ops(group).groupBy { it.tags.single() }.forEach { (tag, tagOps) ->
                // 문서 순서대로 경로별 가장 앞 화면 ID
                val pathKeys = tagOps.groupBy { it.path }.map { (path, pathOps) -> path to pathOps.minOf { V2SwaggerGroups.ScreenKey.of(it.summary) } }
                assertEquals(pathKeys.sortedBy { it.second }, pathKeys, "$group / $tag 경로 순서")
            }
        }
        // 예: 공연 탐색은 P-1 → P-2 → P-3 → P-5, 선물·티켓탭은 G-7 → G-7a → G-8 순
        val browse = ops(V2ApiArea.USER.group).filter { it.tags.single() == V2ApiTags.BROWSE }.map { it.summary!!.substringBefore("]") + "]" }
        assertEquals(listOf("[P-1]", "[P-2]", "[P-3]", "[P-5]"), browse)
    }

    @Test
    fun `화면 ID 정렬 키 - 접두 문자, 숫자, 하위 표기 순, 화면 ID 없으면 맨 뒤`() {
        val ids = listOf("[G-8] a", "[G-7a] b", "헬스", "[G-10] c", "[G-7] d", "[E-2] e")
        assertEquals(listOf("[E-2] e", "[G-7] d", "[G-7a] b", "[G-8] a", "[G-10] c", "헬스"), ids.sortedBy { V2SwaggerGroups.ScreenKey.of(it) })
    }

    @Test
    fun `v1·internal 태그는 예전처럼 이름순`() {
        for (group in listOf("v1", "internal")) {
            val tags = docs(group)["tags"].map { it["name"].asText() }
            assertEquals(tags.sorted(), tags, group)
            assertTrue(ops(group).flatMap { it.tags }.toSet().all { it in tags }, "$group 의 쓰인 태그가 tags 배열에 모두 있음")
        }
    }

    companion object {
        /** `[호스팅] 1. 호스트·멤버` */
        private val TAG_FORMAT = Regex("""^\[(호스팅|사용자)] (\d+)\. \S.*$""")

        /** `[H-1]`, `[O-0]`, `[G-7a]`(문서 화면의 하위 API) 등 */
        private val SCREEN_ID = Regex("""^\[[A-Z]-\d+[a-z]?]""")
    }
}
