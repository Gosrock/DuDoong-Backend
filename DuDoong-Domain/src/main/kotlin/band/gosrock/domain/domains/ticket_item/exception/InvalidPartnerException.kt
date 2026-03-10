package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidPartnerException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_PARTNER) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidPartnerException()
    }
}
