package band.gosrock.api.issuedTicket.dto.response

import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.common.vo.IssuedTicketOptionAnswerVo
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer

data class RetrieveIssuedTicketDTO(
    val issuedTicketInfo: IssuedTicketInfoVo,
    val issuedTicketOptionAnswers: List<IssuedTicketOptionAnswerVo>,
) {
    companion object {
        @JvmStatic
        fun of(issuedTicket: IssuedTicket): RetrieveIssuedTicketDTO {
            return RetrieveIssuedTicketDTO(
                issuedTicketInfo = issuedTicket.toIssuedTicketInfoVo(),
                issuedTicketOptionAnswers = issuedTicket.issuedTicketOptionAnswers
                    .map(IssuedTicketOptionAnswer::toIssuedTicketOptionAnswerVo),
            )
        }
    }
}
