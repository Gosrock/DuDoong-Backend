package band.gosrock.domain.architecture

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.gift.domain.TicketGift
import band.gosrock.domain.domains.gift.repository.TicketGiftRepository
import band.gosrock.domain.domains.gift.service.TicketGiftGuard
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import band.gosrock.domain.domains.order.repository.OrderRefundAccountRepository
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import jakarta.persistence.Entity
import org.junit.jupiter.api.Assertions.assertEquals
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

    /**
     * 목록과 무관한 일반 규칙 (#721): `@Entity` 클래스의 Kotlin `internal` 메서드(JVM 이름 `name$DuDoong_Domain`)는 전부
     * `service.v2` 또는 그 엔티티 자신(중첩 클래스 포함)만 호출한다. 목록에 추가하는 것을 잊은 새 internal mutator 도 여기서 잡힌다.
     * internal 프로퍼티 접근자(`getX$DuDoong_Domain`)도 같은 규칙이 적용된다.
     */
    @Test
    fun `엔티티의 모든 internal 메서드는 v2 도메인 서비스와 엔티티 자신만 호출한다 (자동 수집)`() {
        noClasses().that().resideOutsideOfPackage(DOMAIN_SERVICE_V2)
            .should().callMethodWhere(ENTITY_INTERNAL_CALL_FROM_OTHER_CLASS)
            .check(classes)
    }

    @Test
    fun `엔티티 internal 메서드 자동 수집 결과가 문서화된 엔티티별 v2 mutator 목록과 같다`() {
        val collected: Map<String, Set<String>> = classes.filter { it.isAnnotatedWith(Entity::class.java) }
            .associate { c -> c.simpleName to c.methods.filter(::isKotlinInternal).map { it.name.substringBefore('$') }.toSortedSet() }
            .filterValues { it.isNotEmpty() }
            .toSortedMap()
        val expected: Map<String, Set<String>> = V2_INTERNAL_MUTATORS_BY_OWNER
            .map { (owner, names) -> owner.simpleName to names.toSortedSet() }.toMap().toSortedMap()
        assertEquals(expected, collected, "엔티티 internal 메서드가 바뀌면 V2_INTERNAL_MUTATORS_BY_OWNER 와 api/v2/README.md 아키텍처 표를 함께 갱신")
    }

    @Test
    fun `환불 계좌 저장소(v2 전용 테이블)는 v2 도메인 서비스만 접근한다 (#718)`() {
        noClasses().that().resideOutsideOfPackage(DOMAIN_SERVICE_V2)
            .and().doNotBelongToAnyOf(OrderRefundAccountRepository::class.java)
            .should().dependOnClassesThat().belongToAnyOf(OrderRefundAccountRepository::class.java)
            .check(classes)
    }

    @Test
    fun `선물 저장소(v2 전용 테이블)는 v2 도메인 서비스와 v1 보호용 TicketGiftGuard 만 접근한다 (#719)`() {
        noClasses().that().resideOutsideOfPackage(DOMAIN_SERVICE_V2)
            .and().doNotBelongToAnyOf(TicketGiftRepository::class.java, TicketGiftGuard::class.java)
            .should().dependOnClassesThat().belongToAnyOf(TicketGiftRepository::class.java)
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

        /** 엔티티별 v2 internal mutator (README 아키텍처 표와 같아야 함) */
        private val V2_INTERNAL_MUTATORS_BY_OWNER: Map<Class<*>, Set<String>> = mapOf(
            Event::class.java to setOf(
                "changeHasTicket", "changeSchedule", "changePosterImage", "changePlace", "replaceContacts", "replaceTagIds", "replaceSections",
            ),
            Host::class.java to setOf("replaceContacts", "getOrInitProfile"),
            // TicketItem (#707)
            TicketItem::class.java to setOf("changeAccountInfo", "changeSupplyCount"),
            // Order (#712, #718)
            Order::class.java to setOf("recordRefuseReasonType", "recordV2Payment", "withdrawByUser", "refuseByHost", "cancelByHost"),
            // 선물 (#719): 수락·반환 때 소유자·uuid 교체, 선물 전이 기록
            IssuedTicket::class.java to setOf("transferOwner"),
            TicketGift::class.java to setOf("accept", "reject", "returnToSender", "cancel", "changeMemo"),
            // 환불 계좌 수정 (#728)
            OrderRefundAccount::class.java to setOf("change"),
        )

        private val MUTATOR_OWNERS = V2_INTERNAL_MUTATORS_BY_OWNER.keys.toTypedArray()

        /** Kotlin internal 은 JVM 이름이 `name$모듈명` 으로 맹글링되므로 `name$` 접두도 같은 메서드로 본다 */
        private val V2_INTERNAL_MUTATOR_CALL: DescribedPredicate<JavaMethodCall> =
            DescribedPredicate.describe("Event/Host/TicketItem/Order/IssuedTicket/TicketGift/OrderRefundAccount 의 v2 internal mutator ($V2_INTERNAL_MUTATORS_BY_OWNER)") { call ->
                val target = call.target
                val names = V2_INTERNAL_MUTATORS_BY_OWNER.entries.firstOrNull { it.key.name == target.owner.name }?.value.orEmpty()
                names.any { target.name == it || target.name.startsWith("${it}\$") }
            }

        /** Kotlin internal 맹글링 접미 (Gradle 모듈 이름 `DuDoong-Domain` → `DuDoong_Domain`) */
        private const val INTERNAL_SUFFIX = "\$DuDoong_Domain"

        private fun isKotlinInternal(method: JavaMethod) =
            method.name.endsWith(INTERNAL_SUFFIX) && JavaModifier.SYNTHETIC !in method.modifiers

        /** 호출 대상이 `@Entity` 의 internal 메서드이고, 호출한 클래스가 그 엔티티(또는 엔티티의 중첩 클래스)가 아님 */
        private val ENTITY_INTERNAL_CALL_FROM_OTHER_CLASS: DescribedPredicate<JavaMethodCall> =
            DescribedPredicate.describe("다른 클래스에서 @Entity 의 Kotlin internal 메서드(*$INTERNAL_SUFFIX) 호출") { call ->
                val owner = call.targetOwner
                val origin = call.originOwner.name
                owner.isAnnotatedWith(Entity::class.java) &&
                    call.target.name.endsWith(INTERNAL_SUFFIX) &&
                    origin != owner.name && !origin.startsWith("${owner.name}\$")
            }

        private val classes: JavaClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("band.gosrock.domain")
    }
}
