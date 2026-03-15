package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotEntranceException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.CAN_NOT_ENTRANCE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotEntranceException()
    }
}
