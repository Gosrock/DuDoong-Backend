package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyDeletedStatusException private constructor() : DuDoongCodeException(EventErrorCode.ALREADY_DELETED_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyDeletedStatusException()
    }
}
