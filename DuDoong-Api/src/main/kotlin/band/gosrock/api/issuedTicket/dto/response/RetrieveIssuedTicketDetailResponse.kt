package band.gosrock.api.issuedTicket.dto.response

import band.gosrock.domain.common.vo.EventInfoVo
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket

data class RetrieveIssuedTicketDetailResponse(
    val issuedTicketInfo: IssuedTicketInfoVo,
    val eventInfo: EventInfoVo,
) {
    companion object {
        @JvmStatic
        fun of(issuedTicket: IssuedTicket, event: Event): RetrieveIssuedTicketDetailResponse {
            return RetrieveIssuedTicketDetailResponse(
                issuedTicketInfo = issuedTicket.toIssuedTicketInfoVo(),
                eventInfo = event.toEventInfoVo(),
            )
        }
    }
}
