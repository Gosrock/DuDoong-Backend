package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotCorrectOptionAnswerException private constructor() : DuDoongCodeException(TicketItemErrorCode.OPTION_ANSWER_NOT_CORRECT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotCorrectOptionAnswerException()
    }
}
