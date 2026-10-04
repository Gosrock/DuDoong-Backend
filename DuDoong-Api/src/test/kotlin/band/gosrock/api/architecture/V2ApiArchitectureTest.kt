package band.gosrock.api.architecture

import band.gosrock.api.config.SwaggerConfig
import band.gosrock.api.config.response.GlobalExceptionHandler
import band.gosrock.api.v2.common.V2ErrorPolicy
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * v1 / v2 경계 (DEC-018, #708). 규칙은 `api/v2/README.md` 참고.
 * Api 모듈 classpath 의 운영 코드(Api + Admin + Domain + Infrastructure + Common)를 검사한다. 테스트 코드는 제외.
 */
@DisplayName("v1/v2 API 경계 아키텍처")
class V2ApiArchitectureTest {

    @Test
    fun `v1 api 는 api v2 에 의존하지 않는다 (GlobalExceptionHandler, SwaggerConfig 는 아래 규칙으로 따로 검사)`() {
        noClasses().that().resideInAPackage(API)
            .and().resideOutsideOfPackage(API_V2)
            .and().doNotBelongToAnyOf(*V2_ERROR_POLICY_USERS)
            .should().dependOnClassesThat().resideInAPackage(API_V2)
            .check(classes)
    }

    @Test
    fun `GlobalExceptionHandler, SwaggerConfig 는 api v2 중 V2ErrorPolicy(403 정책)에만 의존한다`() {
        noClasses().that().belongToAnyOf(*V2_ERROR_POLICY_USERS)
            .should().dependOnClassesThat(resideInAPackage(API_V2).and(not(belongToAnyOf(V2ErrorPolicy::class.java))))
            .check(classes)
    }

    @Test
    fun `api v2 는 v1 api(controller, usecase, dto 등)에 의존하지 않는다 (api common, config 는 허용)`() {
        noClasses().that().resideInAPackage(API_V2)
            .should().dependOnClassesThat(
                resideInAPackage(API).and(resideOutsideOfPackages(API_V2, "band.gosrock.api.common..", "band.gosrock.api.config..")),
            )
            .check(classes)
    }

    @Test
    fun `v2 도메인 서비스에는 api v2 와 v2 도메인 서비스만 의존한다 (v1 api, Admin, Domain, Infrastructure, Common 금지)`() {
        classes().that().resideInAPackage(DOMAIN_SERVICE_V2)
            .should().onlyHaveDependentClassesThat().resideInAnyPackage(API_V2, DOMAIN_SERVICE_V2)
            .check(classes)
    }

    companion object {
        private const val API = "band.gosrock.api.."
        private const val API_V2 = "band.gosrock.api.v2.."
        private const val DOMAIN_SERVICE_V2 = "band.gosrock.domain..service.v2.."

        /** V2ErrorPolicy 를 쓰는 공통 설정 (403 정책 / v2 Swagger 그룹 에러 예시) */
        private val V2_ERROR_POLICY_USERS = arrayOf(GlobalExceptionHandler::class.java, SwaggerConfig::class.java)

        private val classes: JavaClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("band.gosrock")
    }
}
