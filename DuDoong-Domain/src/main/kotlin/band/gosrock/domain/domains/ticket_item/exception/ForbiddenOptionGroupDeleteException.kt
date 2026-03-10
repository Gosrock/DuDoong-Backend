package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenOptionGroupDeleteException private constructor() : DuDoongCodeException(TicketItemErrorCode.FORBIDDEN_OPTION_GROUP_DELETE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenOptionGroupDeleteException()
    }
}
