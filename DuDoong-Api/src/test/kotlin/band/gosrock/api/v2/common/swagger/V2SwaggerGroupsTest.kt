package band.gosrock.api.v2.common.swagger

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.common.annotation.CurrentUserId
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
import org.springframework.core.DefaultParameterNameDiscoverer
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

    /** 핸들러 매핑의 v2 (메서드 경로) → 핸들러 메서드 */
    private fun v2HandlerMethods(): Map<String, java.lang.reflect.Method> =
        handlerMapping.handlerMethods.flatMap { (info, handler) ->
            val patterns = info.pathPatternsCondition?.patternValues ?: info.patternsCondition?.patterns.orEmpty()
            patterns.filter { it.startsWith("/api/v2/") }.flatMap { p -> info.methodsCondition.methods.map { "${it.name} $p" to handler.method } }
        }.toMap()

    @Test
    fun `v2 경로는 컨트롤러 영역 그룹에 들어가고 @V2AlsoIn 이면 다른 영역 그룹에도, 합치면 v2-전체·핸들러 매핑과 같다`() {
        val all = ops(V2SwaggerGroups.ALL).map { it.key }.toSet()
        val hosting = ops(V2ApiArea.HOSTING.group).map { it.key }.toSet()
        val user = ops(V2ApiArea.USER.group).map { it.key }.toSet()
        assertEquals(v2Handlers().keys, all, "v2-전체 문서 = 핸들러 매핑")
        assertEquals(all, hosting + user, "어느 영역에도 없음: ${all - hosting - user}")
        // 영역 분류 기준(컨트롤러 영역 + @V2AlsoIn)과 그룹 내용이 같다
        v2HandlerMethods().forEach { (key, method) ->
            val areas = V2ApiArea.areasOf(method)
            assertEquals(V2ApiArea.HOSTING in areas, key in hosting, "$key 호스팅 그룹 (영역 $areas)")
            assertEquals(V2ApiArea.USER in areas, key in user, "$key 사용자앱 그룹 (영역 $areas)")
        }
        // 같은 경로라도 영역이 다르면 나뉜다 (P-2 공연 리스트 / E-2 공연 생성)
        assertTrue("GET /api/v2/events" in user && "POST /api/v2/events" in hosting && "POST /api/v2/events" !in user)
        // 두 앱이 함께 쓰는 API 는 사용자앱 그룹에도 (#755 C09)
        val shared = setOf(
            "GET /api/v2/me/notifications", "GET /api/v2/me/notifications/unread-count", "POST /api/v2/me/notifications/read",
            "POST /api/v2/check-ins/self", "GET /api/v2/hosts/{hostId}", "PUT /api/v2/hosts/{hostId}/follow", "DELETE /api/v2/hosts/{hostId}/follow",
            "GET /api/v2/hosts/{hostId}/events", "GET /api/v2/events/{eventId}/sections", "GET /api/v2/tags",
        )
        assertEquals(shared, hosting intersect user, "두 그룹에 모두 보이는 API")
    }

    @Test
    fun `v2 태그는 하나씩, '(영역) 번호 이름' 형식이고 영역이 그룹과 맞으며 v1 태그가 섞이지 않는다`() {
        for (area in V2ApiArea.entries) {
            ops(area.group).forEach { op ->
                assertEquals(1, op.tags.size, "${op.key} 태그 ${op.tags}")
                val tag = op.tags.single()
                val match = TAG_FORMAT.matchEntire(tag)
                assertNotNull(match, "${op.key} 태그 형식: $tag")
                // 태그는 컨트롤러 영역 그대로 (@V2AlsoIn 으로 다른 그룹에 보여도)
                val primary = V2ApiArea.of(v2Handlers().getValue(op.key))!!
                assertEquals(primary.tagPrefix, "[${match!!.groupValues[1]}]", "${op.key} 의 태그 $tag 가 컨트롤러 영역 $primary 과 다름")
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
                val pathKeys = tagOps.groupBy { it.path }.map { (path, pathOps) -> path to pathOps.minOf { V2SwaggerGroups.ScreenKey.of(it.summary, V2ApiTags.PREFIX_ORDER[tag].orEmpty()) } }
                assertEquals(pathKeys.sortedBy { it.second }, pathKeys, "$group / $tag 경로 순서")
            }
        }
        // 예: 공연 탐색은 P-1 → P-2 → P-3 → P-5, 선물·티켓탭은 G-7 → G-7a → G-8 순
        val browse = ops(V2ApiArea.USER.group).filter { it.tags.single() == V2ApiTags.BROWSE }.map { it.summary!!.substringBefore("]") + "]" }
        assertEquals(listOf("[P-1]", "[P-2]", "[P-3]", "[P-5]"), browse)
        // 태그별 접두 순서 (V2ApiTags.PREFIX_ORDER): 운영은 D → R → F, 티켓은 T-* 다음 O-5
        fun prefixes(tag: String) = ops(V2ApiArea.HOSTING.group).filter { it.tags.single() == tag }.map { it.summary!![1] }.distinct()
        assertEquals(listOf('D', 'R', 'F'), prefixes(V2ApiTags.OPERATION_ORDER))
        assertEquals(listOf('T', 'O'), prefixes(V2ApiTags.TICKET))
    }

    @Test
    fun `화면 ID 정렬 키 - 접두 문자(태그별 순서 지정 가능), 숫자, 하위 표기 순, 화면 ID 없으면 맨 뒤`() {
        val ids = listOf("[G-8] a", "[G-7a] b", "헬스", "[G-10] c", "[G-7] d", "[E-2] e")
        assertEquals(listOf("[E-2] e", "[G-7] d", "[G-7a] b", "[G-8] a", "[G-10] c", "헬스"), ids.sortedBy { V2SwaggerGroups.ScreenKey.of(it) })
        // 태그별 접두 순서: 목록에 있는 문자가 앞(그 순서), 나머지는 뒤에 알파벳순
        val operation = listOf("[F-1] a", "[R-2] b", "[D-1] c", "[R-1] d", "[A-1] e")
        assertEquals(listOf("[D-1] c", "[R-1] d", "[R-2] b", "[F-1] a", "[A-1] e"), operation.sortedBy { V2SwaggerGroups.ScreenKey.of(it, "DRF") })
    }

    @Test
    fun `v1·internal 태그는 예전처럼 이름순`() {
        for (group in listOf("v1", "internal")) {
            val tags = docs(group)["tags"].map { it["name"].asText() }
            assertEquals(tags.sorted(), tags, group)
            assertTrue(ops(group).flatMap { it.tags }.toSet().all { it in tags }, "$group 의 쓰인 태그가 tags 배열에 모두 있음")
        }
    }

    @Test
    fun `@CurrentUserId 는 어느 그룹에도 요청 파라미터로 보이지 않는다 - 토큰에서 채우는 값 (#752)`() {
        // 컨트롤러의 @CurrentUserId 인자 이름 (userId · currentUserId · adminUserId)
        val tokenParams = handlerMapping.handlerMethods.values.flatMap { handler ->
            handler.methodParameters.filter { it.hasParameterAnnotation(CurrentUserId::class.java) }
                .map { it.also { p -> p.initParameterNameDiscovery(DefaultParameterNameDiscoverer()) }.parameterName }
        }.toSet()
        assertEquals(setOf("userId", "currentUserId", "adminUserId"), tokenParams)
        for (group in listOf(V2SwaggerGroups.ALL, V2ApiArea.HOSTING.group, V2ApiArea.USER.group, "v1", "internal")) {
            val params = docs(group)["paths"].flatMap { item -> item.flatMap { op -> op["parameters"]?.toList().orEmpty() } }
            assertTrue(params.isNotEmpty(), "$group 에 파라미터가 하나도 없음 — 문서 생성 확인")
            val leaked = params.filter { it["in"]?.asText() != "path" && it["name"]?.asText() in tokenParams }
            assertEquals(emptyList<JsonNode>(), leaked, "$group 에 @CurrentUserId 가 파라미터로 보임")
        }
        // 이름이 같은 진짜 경로 변수는 그대로 (H-10 멤버 역할 변경 `{userId}`)
        val roleChange = docs(V2SwaggerGroups.ALL)["paths"]["/api/v2/hosts/{hostId}/members/{userId}/role"]["patch"]["parameters"]
        assertTrue(roleChange.any { it["name"].asText() == "userId" && it["in"].asText() == "path" && it["required"].asBoolean() })
    }

    @Test
    fun `날짜는 Swagger 에도 실제 형식 yyyy_MM_dd HH_mm 으로 (date-time 표기 없음, #755 C22)`() {
        val dateTimes = mutableListOf<String>()
        docs(V2SwaggerGroups.ALL)["components"]["schemas"].fields().forEach { (name, schema) ->
            schema["properties"]?.fields()?.forEach { (field, prop) ->
                if (prop["format"]?.asText() == "date-time") dateTimes += "$name.$field"
            }
        }
        assertEquals(emptyList<String>(), dateTimes)
        val listItem = docs(V2SwaggerGroups.ALL)["components"]["schemas"]["V2EventListItemResponse"]["properties"]["startAt"]
        assertEquals("yyyy.MM.dd HH:mm", listItem["pattern"].asText())
    }

    @Test
    fun `요청 역할 enum 은 실제로 받는 영문 값(한글 아님), 응답 역할에도 허용값 (#755 C03)`() {
        val schemas = docs(V2SwaggerGroups.ALL)["components"]["schemas"]
        for (request in listOf("V2UpdateHostMemberRoleRequest", "V2AddHostMemberRequest")) {
            // MASTER 는 받되 HOST_400_10 (예전과 같은 동작) — 한글 표시값이 아니라 실제로 받는 영문 이름
            assertEquals(listOf("MASTER", "MANAGER", "GUEST"), schemas[request]["properties"]["role"]["enum"].map { it.asText() }, request)
        }
        assertEquals(listOf("MASTER", "MANAGER", "GUEST"), schemas["V2HostMemberResponse"]["properties"]["role"]["enum"].map { it.asText() })
    }

    @Test
    fun `성공 응답은 공통 래퍼, 엑셀은 xlsx, 에러는 공통 형식 또는 에러 코드 예시, operationId 는 이름이 겹치지 않는다 (#755 C10·C26·C27)`() {
        val docs = docs(V2SwaggerGroups.ALL)
        val me = docs["paths"]["/api/v2/me"]["get"]["responses"]
        val wrapper = me["200"]["content"]["application/json"]["schema"]
        assertEquals(setOf("success", "status", "data", "timeStamp"), wrapper["properties"].fieldNames().asSequence().toSet())
        assertTrue(wrapper["properties"]["data"]["\$ref"].asText().endsWith("V2MeResponse"), wrapper.toString())
        assertTrue(me.has("4XX"), "공통 에러 응답")
        assertEquals(setOf("success", "status", "code", "reason", "timeStamp", "path"), me["4XX"]["content"]["application/json"]["schema"]["properties"].fieldNames().asSequence().toSet())
        for (path in listOf("/api/v2/events/{eventId}/orders/export", "/api/v2/events/{eventId}/issued-tickets/export")) {
            val content = docs["paths"][path]["get"]["responses"]["200"]["content"]
            assertEquals(listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), content.fieldNames().asSequence().toList(), path)
        }
        // 선물 API 는 에러 코드 예시 (Gift_*)
        val accept = docs["paths"]["/api/v2/gifts/{giftToken}/accept"]["post"]["responses"]
        assertTrue(accept["400"]["content"]["application/json"]["examples"].has("Gift_400_5"), accept.toString())
        val operationIds = docs["paths"].flatMap { item -> item.toList().mapNotNull { it["operationId"]?.asText() } }
        assertEquals(operationIds.size, operationIds.toSet().size, "operationId 중복")
        assertTrue(operationIds.none { Regex("_\\d+$").containsMatchIn(it) }, "자동 접미 operationId: ${operationIds.filter { Regex("_\\d+$").containsMatchIn(it) }}")
        assertTrue(listOf("getHostImageUploadUrl", "getEventImageUploadUrl", "getProfileImageUploadUrl").all { it in operationIds })
    }

    companion object {
        /** `[호스팅] 1. 호스트·멤버` */
        private val TAG_FORMAT = Regex("""^\[(호스팅|사용자)] (\d+)\. \S.*$""")

        /** `[H-1]`, `[O-0]`, `[G-7a]`(문서 화면의 하위 API) 등 */
        private val SCREEN_ID = Regex("""^\[[A-Z]-\d+[a-z]?]""")
    }
}
