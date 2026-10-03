package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class UnsupportedV2TicketPayTypeException private constructor() : DuDoongCodeException(TicketItemErrorCode.UNSUPPORTED_V2_TICKET_PAY_TYPE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = UnsupportedV2TicketPayTypeException()
    }
}
