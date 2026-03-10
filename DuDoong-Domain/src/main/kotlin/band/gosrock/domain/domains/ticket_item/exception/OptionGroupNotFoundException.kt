package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class OptionGroupNotFoundException private constructor() : DuDoongCodeException(TicketItemErrorCode.OPTION_GROUP_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OptionGroupNotFoundException()
    }
}
