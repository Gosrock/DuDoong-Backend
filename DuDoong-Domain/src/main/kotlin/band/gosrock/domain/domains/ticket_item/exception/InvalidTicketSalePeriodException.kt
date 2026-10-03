package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidTicketSalePeriodException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_TICKET_SALE_PERIOD) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidTicketSalePeriodException()
    }
}
