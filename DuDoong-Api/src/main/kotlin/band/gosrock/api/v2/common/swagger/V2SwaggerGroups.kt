package band.gosrock.api.v2.common.swagger

import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * v2 Swagger 그룹 (#731): `v2-전체`(기본 선택, application.yml urls-primary-name) / `v2-호스팅센터` / `v2-사용자앱`. v1·internal 그룹은 `SwaggerConfig` 그대로.
 * 영역 그룹은 컨트롤러 단위로 나눈다 ([V2ApiArea.of] — 패키지 기본값 + [V2Area] 예외). 경로만으로는 `GET /api/v2/events`(P-2) 와 `POST /api/v2/events`(E-2) 처럼 나눌 수 없다
 */
@Configuration
@Profile("!staging & !prod")
class V2SwaggerGroups {

    @Bean
    fun v2AllApi(): GroupedOpenApi = group(ALL, null, "v2 전체 (호스팅 센터 + 사용자 앱).")

    @Bean
    fun v2HostingApi(): GroupedOpenApi = group(V2ApiArea.HOSTING.group, V2ApiArea.HOSTING, "호스팅 센터 (호스트·공연 준비·티켓·공연 운영·알림센터). 와이어프레임 10 문서 화면 ID.")

    @Bean
    fun v2UserApi(): GroupedOpenApi = group(V2ApiArea.USER.group, V2ApiArea.USER, "사용자 앱 (공연 탐색·주문·티켓탭·마이페이지). 와이어프레임 11 문서 화면 ID.")

    private fun group(name: String, area: V2ApiArea?, summary: String): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group(name)
            .pathsToMatch("/api/v2/**")
            .apply { if (area != null) addOpenApiMethodFilter { V2ApiArea.of(it.declaringClass) == area } }
            .addOpenApiCustomizer { openApi ->
                openApi.info(Info().title("두둥 v2 API - $name").version("v2").description("$summary\n\n$GUIDE"))
                openApi.tags = openApi.tags?.sortedBy { tag -> V2ApiTags.ORDERED.indexOf(tag.name).let { if (it < 0) Int.MAX_VALUE else it } }
                openApi.paths?.let { paths -> openApi.paths = Paths().apply { sortPaths(paths).forEach { path -> addPathItem(path, paths[path]) } } }
            }
            .build()

    /**
     * 경로 순서. Swagger UI 는 태그별로 문서의 경로 순서대로 보여 주고, 같은 경로의 메서드는 묶는다. 그래서 태그마다
     * "그 태그 operation 들의 화면 ID 중 가장 앞" 순으로 경로가 놓이도록, 태그별 순서를 제약으로 위상 정렬한다.
     * 한 경로가 여러 태그에 걸치면(`/api/v2/events`: P-2 탐색 + E-2 공연 준비) 두 태그의 순서를 모두 만족시키고, 동점·나머지는 전체 화면 ID 순.
     * 제약이 순환하면(실제로는 없음) 전체 화면 ID 순으로 대신한다
     */
    private fun sortPaths(paths: Paths): List<String> {
        fun key(item: PathItem, tag: String? = null) =
            item.readOperations().filter { tag == null || tag in it.tags.orEmpty() }
                .minOfOrNull { ScreenKey.of(it.summary, tag?.let { t -> V2ApiTags.PREFIX_ORDER[t] }.orEmpty()) } ?: ScreenKey.NONE
        val globalOrder = compareBy<String> { key(paths.getValue(it)) }.thenBy { it }
        val tags = paths.values.flatMap { it.readOperations() }.flatMap { it.tags.orEmpty() }.distinct()
        val after = paths.keys.associateWith { mutableSetOf<String>() }
        val inDegree = paths.keys.associateWith { 0 }.toMutableMap()
        tags.forEach { tag ->
            paths.keys.filter { path -> paths.getValue(path).readOperations().any { tag in it.tags.orEmpty() } }
                .sortedWith(compareBy<String> { key(paths.getValue(it), tag) }.then(globalOrder))
                .zipWithNext()
                .forEach { (before, next) -> if (after.getValue(before).add(next)) inDegree[next] = inDegree.getValue(next) + 1 }
        }
        val ready = java.util.PriorityQueue(globalOrder).apply { addAll(inDegree.filterValues { it == 0 }.keys) }
        val sorted = mutableListOf<String>()
        while (ready.isNotEmpty()) {
            val path = ready.poll().also { sorted += it }
            after.getValue(path).forEach { next -> inDegree[next] = inDegree.getValue(next) - 1; if (inDegree.getValue(next) == 0) ready += next }
        }
        return if (sorted.size == paths.size) sorted else paths.keys.sortedWith(globalOrder)
    }

    /**
     * summary 앞 화면 ID `[P-1]`, `[G-7a]` 의 정렬 키: 접두 문자 → 숫자 → 하위 표기. 화면 ID 가 없으면 맨 뒤.
     * 접두 문자 순서는 [prefixRank] — 태그별 순서([V2ApiTags.PREFIX_ORDER])에 있으면 그 자리, 없으면 그 뒤에 알파벳순
     */
    data class ScreenKey(val prefixRank: Int, val number: Int, val suffix: String) : Comparable<ScreenKey> {
        override fun compareTo(other: ScreenKey): Int = compareValuesBy(this, other, { it.prefixRank }, { it.number }, { it.suffix })

        companion object {
            private val SCREEN_ID = Regex("""^\[([A-Z])-(\d+)([a-z]?)]""")
            val NONE = ScreenKey(Int.MAX_VALUE, Int.MAX_VALUE, "")

            fun of(summary: String?, prefixOrder: String = ""): ScreenKey =
                summary?.let { SCREEN_ID.find(it) }?.let {
                    val prefix = it.groupValues[1].single()
                    val rank = prefixOrder.indexOf(prefix).let { i -> if (i >= 0) i else prefixOrder.length + prefix.code }
                    ScreenKey(rank, it.groupValues[2].toInt(), it.groupValues[3])
                } ?: NONE
        }
    }

    companion object {
        const val ALL = "v2-전체"

        /** 그룹 설명 공통: 권한 표기·응답·에러 코드 안내 */
        val GUIDE = """
            |**operation summary** 앞의 `[H-1]` 등은 와이어프레임 API 문서(WorkBook v2/10 호스팅센터, v2/11 사용자앱)의 화면 ID 다. 태그 번호는 와이어프레임 순서.
            |
            |**권한 표기**: P = 비로그인 허용, U = 로그인, G+ = 호스트 일반 멤버 이상, M+ = 매니저 이상, MS = 마스터만. SUPER_ADMIN 은 G+/M+/MS 검사를 건너뛴다.
            |
            |**응답**: 성공은 `{success, status, data, timeStamp}`, 실패는 `{success, status, code, reason, timeStamp, path}`.
            |비로그인 401, 호스트 권한 부족은 v2 에서만 403. 엑셀 다운로드는 래핑 없이 xlsx.
            |
            |**에러 코드**: `code` 는 `{도메인}_{HTTP 상태}_{번호}`(예: `Order_400_15`). 주요 코드는 각 API 설명에 적었고, 도메인별 전체 목록은 v1 그룹의 `xx. [예시] 에러코드 문서화` 태그에서 본다.
        """.trimMargin()
    }
}
