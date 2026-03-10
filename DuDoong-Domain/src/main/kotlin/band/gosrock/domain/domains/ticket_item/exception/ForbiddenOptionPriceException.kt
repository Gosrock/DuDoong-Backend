package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenOptionPriceException private constructor() : DuDoongCodeException(TicketItemErrorCode.FORBIDDEN_OPTION_PRICE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenOptionPriceException()
    }
}
