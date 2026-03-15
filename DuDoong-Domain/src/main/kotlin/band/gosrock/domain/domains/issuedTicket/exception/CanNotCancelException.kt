package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotCancelException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.CAN_NOT_CANCEL) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotCancelException()
    }
}
