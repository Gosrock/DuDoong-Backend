package band.gosrock.domain.domains.issuedTicket.dto.request

import org.jetbrains.annotations.NotNull

class CreateIssuedTicketForDevDTO {
    @NotNull
    var eventId: Long? = null

    @NotNull
    var orderLineId: Long? = null

    @NotNull
    var ticketItemId: Long? = null

    @NotNull
    var amount: Long? = null

    @NotNull
    var optionAnswers: List<Long>? = null
}
