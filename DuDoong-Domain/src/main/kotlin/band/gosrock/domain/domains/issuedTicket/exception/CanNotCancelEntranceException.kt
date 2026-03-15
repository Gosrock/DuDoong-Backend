package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotCancelEntranceException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.CAN_NOT_CANCEL_ENTRANCE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotCancelEntranceException()
    }
}
