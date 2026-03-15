package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer

data class IssuedTicketOptionAnswerVo(
    val issuedTicketOptionAnswerId: Long? = null,
    val optionQuestion: String? = null,
    val answer: String? = null,
    val additionalPrice: Money? = null,
) {
    companion object {
        @JvmStatic
        fun from(issuedTicketOptionAnswer: IssuedTicketOptionAnswer): IssuedTicketOptionAnswerVo =
            IssuedTicketOptionAnswerVo(
                issuedTicketOptionAnswerId = issuedTicketOptionAnswer.id,
                answer = issuedTicketOptionAnswer.answer,
                additionalPrice = issuedTicketOptionAnswer.additionalPrice,
            )
    }
}
