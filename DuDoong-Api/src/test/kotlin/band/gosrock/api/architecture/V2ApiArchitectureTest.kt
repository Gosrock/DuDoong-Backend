package band.gosrock.api.architecture

import band.gosrock.api.config.SwaggerConfig
import band.gosrock.api.config.response.GlobalExceptionHandler
import band.gosrock.api.v2.common.V2ErrorPolicy
import band.gosrock.api.v2.event.controller.V2EventBrowseController
import band.gosrock.api.v2.event.dto.response.V2EventDetailResponse
import band.gosrock.api.v2.event.dto.response.V2EventHostSummaryResponse
import band.gosrock.api.v2.event.dto.response.V2EventListItemResponse
import band.gosrock.api.v2.event.dto.response.V2HomeEventResponse
import band.gosrock.api.v2.event.dto.response.V2HomeResponse
import band.gosrock.api.v2.event.dto.response.V2PublicTicketItemResponse
import band.gosrock.api.v2.event.dto.response.V2PublicTicketOptionResponse
import band.gosrock.api.v2.event.usecase.V2ReadEventDetailUseCase
import band.gosrock.api.v2.event.usecase.V2ReadHomeUseCase
import band.gosrock.api.v2.event.usecase.V2ReadOnSaleTicketItemsUseCase
import band.gosrock.api.v2.event.usecase.V2SearchEventsUseCase
import band.gosrock.api.v2.ticket.dto.response.V2TicketAccountResponse
import band.gosrock.domain.common.vo.AccountInfoVo
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import jakarta.persistence.Entity
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

    @Test
    fun `공개 공연 탐색(P-1~P-5) 은 계좌 정보(AccountInfoVo, V2TicketAccountResponse)에 의존하지 않는다 (#716, 계좌는 주문 단계)`() {
        noClasses().that().belongToAnyOf(*PUBLIC_BROWSE_CLASSES)
            .should().dependOnClassesThat().belongToAnyOf(AccountInfoVo::class.java, V2TicketAccountResponse::class.java)
            .check(classes)
    }

    @Test
    fun `공개 공연 탐색 응답 DTO 는 JPA 엔티티(@Entity)에 의존하지 않는다 (#716, 엔티티 직렬화·필드 누출 방지)`() {
        noClasses().that().belongToAnyOf(*PUBLIC_BROWSE_RESPONSES)
            .should().dependOnClassesThat().areAnnotatedWith(Entity::class.java)
            .check(classes)
    }

    companion object {
        private const val API = "band.gosrock.api.."
        private const val API_V2 = "band.gosrock.api.v2.."
        private const val DOMAIN_SERVICE_V2 = "band.gosrock.domain..service.v2.."

        /** V2ErrorPolicy 를 쓰는 공통 설정 (403 정책 / v2 Swagger 그룹 에러 예시) */
        private val V2_ERROR_POLICY_USERS = arrayOf(GlobalExceptionHandler::class.java, SwaggerConfig::class.java)

        /** 비로그인 공개 공연 탐색 응답 DTO */
        private val PUBLIC_BROWSE_RESPONSES = arrayOf(
            V2HomeResponse::class.java,
            V2HomeEventResponse::class.java,
            V2EventListItemResponse::class.java,
            V2EventDetailResponse::class.java,
            V2EventHostSummaryResponse::class.java,
            V2PublicTicketItemResponse::class.java,
            V2PublicTicketOptionResponse::class.java,
        )

        /** 비로그인 공개 공연 탐색 API (컨트롤러·유스케이스 + 응답 DTO) */
        private val PUBLIC_BROWSE_CLASSES = arrayOf(
            V2EventBrowseController::class.java,
            V2ReadHomeUseCase::class.java,
            V2SearchEventsUseCase::class.java,
            V2ReadEventDetailUseCase::class.java,
            V2ReadOnSaleTicketItemsUseCase::class.java,
            *PUBLIC_BROWSE_RESPONSES,
        )

        private val classes: JavaClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("band.gosrock")
    }
}
