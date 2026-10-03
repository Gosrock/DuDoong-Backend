package band.gosrock.api.v2.common

import band.gosrock.api.example.controller.ExampleController
import band.gosrock.api.v2.health.controller.V2HealthController
import band.gosrock.domain.domains.host.exception.HostErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("V2ErrorPolicy.resolve")
class V2ErrorPolicyTest {

    @ParameterizedTest(name = "{0}: v2 핸들러면 status 만 403, code/reason 유지")
    @EnumSource(HostErrorCode::class, names = ["FORBIDDEN_HOST", "NOT_ACCEPTED_HOST", "NOT_MANAGER_HOST", "NOT_MASTER_HOST", "NOT_PARTNER_HOST"])
    fun v2HandlerForbidden(code: HostErrorCode) {
        val result = V2ErrorPolicy.resolve(V2HealthController::class.java, code)

        assertEquals(code.getErrorReason().copy(status = 403), result)
    }

    @ParameterizedTest(name = "{0}: v1 핸들러면 원래 ErrorReason 그대로")
    @EnumSource(HostErrorCode::class, names = ["FORBIDDEN_HOST", "NOT_ACCEPTED_HOST", "NOT_MANAGER_HOST", "NOT_MASTER_HOST", "NOT_PARTNER_HOST"])
    fun v1HandlerUnchanged(code: HostErrorCode) {
        assertEquals(code.getErrorReason(), V2ErrorPolicy.resolve(ExampleController::class.java, code))
    }

    @Test
    @DisplayName("핸들러가 없으면(null) v2 가 아닌 것으로 처리한다")
    fun noHandler() {
        val code = HostErrorCode.FORBIDDEN_HOST

        assertEquals(code.getErrorReason(), V2ErrorPolicy.resolve(null, code))
    }

    @Test
    @DisplayName("v2 핸들러라도 권한과 무관한 코드는 그대로")
    fun v2HandlerNonPermissionCode() {
        val code = HostErrorCode.HOST_NOT_FOUND

        assertEquals(code.getErrorReason(), V2ErrorPolicy.resolve(V2HealthController::class.java, code))
    }
}
