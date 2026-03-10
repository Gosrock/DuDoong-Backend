package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class UseOtherApiException private constructor() : DuDoongCodeException(EventErrorCode.USE_OTHER_API) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = UseOtherApiException()
    }
}
