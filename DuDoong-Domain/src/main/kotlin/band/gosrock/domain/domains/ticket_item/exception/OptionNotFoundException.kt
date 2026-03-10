package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class OptionNotFoundException private constructor() : DuDoongCodeException(TicketItemErrorCode.OPTION_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OptionNotFoundException()
    }
}
