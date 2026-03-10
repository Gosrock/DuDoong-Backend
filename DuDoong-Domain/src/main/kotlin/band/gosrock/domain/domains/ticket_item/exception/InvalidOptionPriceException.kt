package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidOptionPriceException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_OPTION_PRICE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidOptionPriceException()
    }
}
