package band.gosrock.api.v2.common

import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.domain.domains.host.exception.HostErrorCode

/**
 * v2 핸들러 전용 에러 상태 정책.
 *
 * 호스트 권한 실패는 v1 에서 400 으로 내려가지만 (운영 호환성 유지),
 * v2 핸들러(`band.gosrock.api.v2` 패키지)에서는 의미에 맞게 403 으로 내린다. code / reason 은 그대로 둔다.
 * 요청 경로가 아닌 핸들러 패키지로 판정하므로 v2 컨트롤러는 반드시 이 패키지 아래에 둔다.
 */
object V2ErrorPolicy {
    const val V2_PACKAGE = "band.gosrock.api.v2"

    val HOST_FORBIDDEN_CODES: Set<BaseErrorCode> = setOf(
        HostErrorCode.FORBIDDEN_HOST,
        HostErrorCode.NOT_ACCEPTED_HOST,
        HostErrorCode.NOT_MANAGER_HOST,
        HostErrorCode.NOT_MASTER_HOST,
        HostErrorCode.NOT_PARTNER_HOST,
    )

    private const val FORBIDDEN = 403

    /** @param handlerType 예외를 던진 컨트롤러 타입. 핸들러가 없으면 null (v2 아님으로 처리) */
    fun resolve(handlerType: Class<*>?, errorCode: BaseErrorCode): ErrorReason {
        val errorReason = errorCode.getErrorReason()
        if (isV2Handler(handlerType) && errorCode in HOST_FORBIDDEN_CODES) {
            return errorReason.copy(status = FORBIDDEN)
        }
        return errorReason
    }

    private fun isV2Handler(handlerType: Class<*>?): Boolean {
        val packageName = handlerType?.packageName ?: return false
        return packageName == V2_PACKAGE || packageName.startsWith("$V2_PACKAGE.")
    }
}
