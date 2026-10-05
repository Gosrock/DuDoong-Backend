package band.gosrock.api.v2.common.swagger

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
            .addOpenApiCustomizer { it.info(Info().title("두둥 v2 API - $name").version("v2").description("$summary\n\n$GUIDE")) }
            .build()

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
