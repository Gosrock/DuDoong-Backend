package band.gosrock.api.issuedTicket.dto.request

import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

class AdminIssuedTicketTableQueryRequest(
    val searchType: AdminTableSearchType?,
    @Schema(nullable = true)
    val searchString: String?,
) {
    @JsonIgnore
    fun toCondition(eventId: Long): FindEventIssuedTicketsCondition {
        return FindEventIssuedTicketsCondition(
            eventId = eventId,
            searchString = searchString,
            searchType = searchType,
        )
    }
}
