package band.gosrock.admin.exception

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.infrastructure.outer.aws.StagingServerControlException

/** EC2 제어 실패를 어드민 응답(409/503)으로 바꾼다 */
class StagingServerControlFailedException private constructor(errorCode: AdminErrorCode) :
    DuDoongCodeException(errorCode) {
    companion object {
        @JvmField
        val STATE_CONFLICT: DuDoongCodeException = StagingServerControlFailedException(AdminErrorCode.STAGING_SERVER_STATE_CONFLICT)

        @JvmField
        val UNAVAILABLE: DuDoongCodeException = StagingServerControlFailedException(AdminErrorCode.STAGING_SERVER_UNAVAILABLE)

        fun from(e: StagingServerControlException): DuDoongCodeException =
            when (e.kind) {
                StagingServerControlException.Kind.STATE_CONFLICT -> STATE_CONFLICT
                StagingServerControlException.Kind.UNAVAILABLE -> UNAVAILABLE
            }
    }
}
