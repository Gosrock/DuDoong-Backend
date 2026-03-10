package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class EmptyAccountInfoException private constructor() : DuDoongCodeException(TicketItemErrorCode.EMPTY_ACCOUNT_INFO) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EmptyAccountInfoException()
    }
}
