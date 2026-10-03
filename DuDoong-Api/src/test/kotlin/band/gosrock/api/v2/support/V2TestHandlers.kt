package band.gosrock.api.v2.support

import band.gosrock.common.annotation.ApiErrorCodeExample
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.ForbiddenHostException
import band.gosrock.domain.domains.host.exception.HostErrorCode
import band.gosrock.domain.domains.host.exception.HostNotFoundException
import band.gosrock.domain.domains.host.exception.NotAcceptedHostException
import band.gosrock.domain.domains.host.exception.NotManagerHostException
import band.gosrock.domain.domains.host.exception.NotMasterHostException
import band.gosrock.domain.domains.host.exception.NotPartnerHostException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/** HostErrorCode 이름 → 실제 도메인 예외 */
val HOST_ERROR_EXCEPTIONS: Map<String, DuDoongCodeException> = mapOf(
    "FORBIDDEN_HOST" to ForbiddenHostException.EXCEPTION,
    "NOT_ACCEPTED_HOST" to NotAcceptedHostException.EXCEPTION,
    "NOT_MANAGER_HOST" to NotManagerHostException.EXCEPTION,
    "NOT_MASTER_HOST" to NotMasterHostException.EXCEPTION,
    "NOT_PARTNER_HOST" to NotPartnerHostException.EXCEPTION,
    "ALREADY_JOINED_HOST" to AlreadyJoinedHostException.EXCEPTION,
    "HOST_NOT_FOUND" to HostNotFoundException.EXCEPTION,
)

/**
 * `band.gosrock.api.v2` 패키지에 위치해야 하는 테스트 전용 핸들러 모음.
 *
 * 핸들러는 inner class 라서 독립 클래스가 아니므로, 통합 테스트(@ComponentScan band.gosrock)의 스캔 대상에서 제외된다.
 * 홀더 자체도 스테레오타입 어노테이션이 없어 빈으로 등록되지 않는다.
 */
class V2TestHandlers {

    /** v2 패키지 컨트롤러. 경로가 v2 가 아니어도 패키지 기준으로 v2 정책이 적용되는지 확인하기 위해 비-v2 경로도 매핑한다. */
    @RestController
    inner class HostErrorController {
        @GetMapping("/api/v2/test/host-errors/{name}", "/test/v2-handler/host-errors/{name}")
        fun throwError(@PathVariable name: String): Unit = throw HOST_ERROR_EXCEPTIONS.getValue(name)
    }

    /** Swagger 에러 예시 생성 검증용 v2 핸들러 */
    inner class DocsHandler {
        @ApiErrorCodeExample(HostErrorCode::class)
        fun hostErrors() {}
    }
}
