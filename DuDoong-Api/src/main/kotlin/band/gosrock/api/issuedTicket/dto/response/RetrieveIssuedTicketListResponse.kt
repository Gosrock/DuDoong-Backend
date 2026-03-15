package band.gosrock.api.issuedTicket.dto.response

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import org.springframework.data.domain.Page

data class RetrieveIssuedTicketListResponse(
    val page: Int,
    val totalPage: Int,
    val issuedTickets: List<RetrieveIssuedTicketDTO>,
) {
    companion object {
        @JvmStatic
        fun of(issuedTickets: Page<IssuedTicket>): RetrieveIssuedTicketListResponse {
            return RetrieveIssuedTicketListResponse(
                page = issuedTickets.pageable.pageNumber,
                totalPage = issuedTickets.totalPages,
                issuedTickets = issuedTickets.map(RetrieveIssuedTicketDTO::of).toList(),
            )
        }
    }
}
