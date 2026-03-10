package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotAppliedItemOptionGroupException private constructor() : DuDoongCodeException(TicketItemErrorCode.NOT_APPLIED_ITEM_OPTION_GROUP) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotAppliedItemOptionGroupException()
    }
}
