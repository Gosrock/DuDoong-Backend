package band.gosrock.api.v2.common.swagger

/**
 * v2 API 영역 (Swagger 그룹 `v2-호스팅센터` / `v2-사용자앱`, #731).
 *
 * 컨트롤러의 영역은 [V2Area] 어노테이션이 있으면 그 값, 없으면 패키지(`band.gosrock.api.v2.<패키지>`)로 정한다 — 새 컨트롤러도 패키지만 맞으면 자동 분류된다.
 * 어느 쪽에도 해당하지 않는 패키지는 null 이고, `V2SwaggerGroupsTest` 가 실패해 분류를 강제한다.
 * 같은 패키지에 다른 영역 컨트롤러가 섞이면(예: `event` 의 공연 탐색) 클래스에 [V2Area] 를 붙인다.
 */
enum class V2ApiArea(val group: String, val tagPrefix: String) {
    HOSTING("v2-호스팅센터", "[호스팅]"),
    USER("v2-사용자앱", "[사용자]");

    companion object {
        private const val V2_PACKAGE = "band.gosrock.api.v2."

        /** 패키지 기본 영역. 알림센터(N-*)는 호스팅 센터 문서(10)에 정의돼 호스팅에 둔다 (사용자 앱 마이페이지 M-6 도 같은 API) */
        private val PACKAGE_AREAS = mapOf(
            "host" to HOSTING,
            "event" to HOSTING,
            "tag" to HOSTING,
            "ticket" to HOSTING,
            "operation" to HOSTING,
            "notification" to HOSTING,
            "health" to HOSTING,
            "order" to USER,
            "gift" to USER,
            "mypage" to USER,
        )

        /**
         * 한 operation 이 보이는 영역 그룹: 컨트롤러 영역([of]) + [V2AlsoIn] 으로 더한 영역 (#755). 화면이 두 앱에서 쓰이는 API
         * (알림, 셀프 체크인, 호스트 홈·팔로우·호스트 공연, 공연 섹션, 태그)를 사용자앱 그룹에도 보이게 한다. 태그는 원래 영역 그대로
         */
        fun areasOf(method: java.lang.reflect.Method): Set<V2ApiArea> {
            val primary = of(method.declaringClass) ?: return emptySet()
            val also = (method.getAnnotation(V2AlsoIn::class.java) ?: method.declaringClass.getAnnotation(V2AlsoIn::class.java))?.value.orEmpty()
            return setOf(primary) + also
        }

        fun of(controller: Class<*>): V2ApiArea? {
            controller.getAnnotation(V2Area::class.java)?.let { return it.value }
            val name = controller.name
            if (!name.startsWith(V2_PACKAGE)) return null
            return PACKAGE_AREAS[name.removePrefix(V2_PACKAGE).substringBefore('.')]
        }
    }
}

/** 컨트롤러 영역 그룹에 더해 다른 영역 그룹에도 보일 operation(메서드) 또는 컨트롤러 전체에 붙인다 (#755) */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class V2AlsoIn(vararg val value: V2ApiArea)

/** 패키지 기본 영역([V2ApiArea.of])과 다른 영역에 둘 v2 컨트롤러에 붙인다 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class V2Area(val value: V2ApiArea)
