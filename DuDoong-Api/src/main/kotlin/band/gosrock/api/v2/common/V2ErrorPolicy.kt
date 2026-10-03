package band.gosrock.api.v2.common

import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.domain.domains.host.exception.HostErrorCode

/**
 * v2 경로 전용 에러 상태 정책.
 *
 * 호스트 권한 실패는 v1 에서 400 으로 내려가지만 (운영 호환성 유지),
 * v2 에서는 의미에 맞게 403 으로 내린다. code / reason 은 그대로 둔다.
 */
object V2ErrorPolicy {
    const val V2_PATH_PREFIX = "/api/v2/"

    val HOST_FORBIDDEN_CODES: Set<BaseErrorCode> = setOf(
        HostErrorCode.FORBIDDEN_HOST,
        HostErrorCode.NOT_ACCEPTED_HOST,
        HostErrorCode.NOT_MANAGER_HOST,
        HostErrorCode.NOT_MASTER_HOST,
        HostErrorCode.NOT_PARTNER_HOST,
    )

    private const val FORBIDDEN = 403

    fun resolve(requestUri: String, errorCode: BaseErrorCode): ErrorReason {
        val errorReason = errorCode.getErrorReason()
        if (requestUri.startsWith(V2_PATH_PREFIX) && errorCode in HOST_FORBIDDEN_CODES) {
            return errorReason.copy(status = FORBIDDEN)
        }
        return errorReason
    }
}
