package band.gosrock.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** v2 도메인 서비스 경계 (DEC-018, #708). 배치는 v1 경로이므로 v2 도메인 서비스를 쓰지 않는다. 규칙은 Api 모듈 `api/v2/README.md` 참고 */
@DisplayName("배치 v2 도메인 서비스 경계 아키텍처")
class V2BatchArchitectureTest {

    @Test
    fun `v2 도메인 서비스에는 v2 도메인 서비스만 의존한다 (Batch, Domain, Infrastructure, Common 금지)`() {
        val classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("band.gosrock")

        classes().that().resideInAPackage(DOMAIN_SERVICE_V2)
            .should().onlyHaveDependentClassesThat().resideInAnyPackage(DOMAIN_SERVICE_V2)
            .check(classes)
    }

    companion object {
        private const val DOMAIN_SERVICE_V2 = "band.gosrock.domain..service.v2.."
    }
}
