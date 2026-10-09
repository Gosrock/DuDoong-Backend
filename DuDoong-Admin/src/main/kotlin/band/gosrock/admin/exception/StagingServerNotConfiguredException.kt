package band.gosrock.admin.exception

import band.gosrock.common.exception.DuDoongCodeException

class StagingServerNotConfiguredException private constructor() :
    DuDoongCodeException(AdminErrorCode.STAGING_SERVER_NOT_CONFIGURED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = StagingServerNotConfiguredException()
    }
}
