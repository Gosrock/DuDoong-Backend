package band.gosrock.domain.architecture

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** v2 도메인 서비스 경계 (DEC-018, #708). 규칙은 Api 모듈 `api/v2/README.md` 참고. 테스트 코드는 제외 */
@DisplayName("v2 도메인 서비스 경계 아키텍처")
class V2DomainServiceArchitectureTest {

    @Test
    fun `v2 도메인 서비스에는 v2 도메인 서비스만 의존한다 (엔티티, v1 서비스 등 금지)`() {
        // Domain 모듈 classpath 에는 api 가 없으므로 Api 모듈 규칙의 api v2 허용분은 해당 없음
        classes().that().resideInAPackage(DOMAIN_SERVICE_V2)
            .should().onlyHaveDependentClassesThat().resideInAnyPackage(DOMAIN_SERVICE_V2)
            .check(classes)
    }

    @Test
    fun `엔티티의 v2 internal mutator 는 v2 도메인 서비스와 엔티티 자신만 호출한다`() {
        noClasses().that().resideOutsideOfPackage(DOMAIN_SERVICE_V2)
            .and().doNotBelongToAnyOf(*MUTATOR_OWNERS)
            .should().callMethodWhere(V2_INTERNAL_MUTATOR_CALL)
            .check(classes)
    }

    @Test
    fun `V2 로 시작하는 DomainService 는 service v2 패키지에 있다`() {
        classes().that().haveSimpleNameStartingWith("V2").and().haveSimpleNameEndingWith("DomainService")
            .should().resideInAPackage(DOMAIN_SERVICE_V2)
            .check(classes)
    }

    companion object {
        private const val DOMAIN_SERVICE_V2 = "band.gosrock.domain..service.v2.."

        private val MUTATOR_OWNERS = arrayOf(Event::class.java, Host::class.java, TicketItem::class.java, Order::class.java)

        private val V2_INTERNAL_MUTATORS = listOf(
            "changeHasTicket", "changeSchedule", "changePosterImage", "changePlace",
            "replaceContacts", "replaceTagIds", "replaceSections", "getOrInitProfile",
            // TicketItem (#707)
            "changeAccountInfo", "changeSupplyCount",
            // Order (#712)
            "recordRefuseReasonType",
        )

        /** Kotlin internal 은 JVM 이름이 `name$모듈명` 으로 맹글링되므로 `name$` 접두도 같은 메서드로 본다 */
        private val V2_INTERNAL_MUTATOR_CALL: DescribedPredicate<JavaMethodCall> =
            DescribedPredicate.describe("Event/Host/TicketItem/Order 의 v2 internal mutator ($V2_INTERNAL_MUTATORS)") { call ->
                val target = call.target
                target.owner.name in MUTATOR_OWNERS.map { it.name } &&
                    V2_INTERNAL_MUTATORS.any { target.name == it || target.name.startsWith("${it}\$") }
            }

        private val classes: JavaClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("band.gosrock.domain")
    }
}
