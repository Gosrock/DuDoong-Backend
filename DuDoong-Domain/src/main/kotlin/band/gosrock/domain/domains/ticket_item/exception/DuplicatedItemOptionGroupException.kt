package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class DuplicatedItemOptionGroupException private constructor() : DuDoongCodeException(TicketItemErrorCode.DUPLICATED_ITEM_OPTION_GROUP) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = DuplicatedItemOptionGroupException()
    }
}
