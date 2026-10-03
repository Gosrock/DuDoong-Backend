package band.gosrock.domain.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** v2 도메인 서비스 경계 (DEC-018, #708). 규칙은 Api 모듈 `api/v2/README.md` 참고. 테스트 코드는 제외 */
@DisplayName("v2 도메인 서비스 경계 아키텍처")
class V2DomainServiceArchitectureTest {

    @Test
    fun `service v2 밖의 Domain 클래스(엔티티, v1 서비스 등)는 service v2 에 의존하지 않는다`() {
        val classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("band.gosrock.domain")

        noClasses().that().resideInAPackage("band.gosrock.domain..")
            .and().resideOutsideOfPackage(DOMAIN_SERVICE_V2)
            .should().dependOnClassesThat().resideInAPackage(DOMAIN_SERVICE_V2)
            .check(classes)
    }

    companion object {
        private const val DOMAIN_SERVICE_V2 = "band.gosrock.domain..service.v2.."
    }
}
