package band.gosrock.domain.common.events.issuedTicket

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo

data class EntranceIssuedTicketEvent(
    val issuedTicketNo: String,
    val eventId: Long,
    val userInfo: IssuedTicketUserInfoVo?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun from(issuedTicket: IssuedTicket): EntranceIssuedTicketEvent =
            EntranceIssuedTicketEvent(
                eventId = issuedTicket.eventId ?: 0L,
                issuedTicketNo = issuedTicket.issuedTicketNo ?: "",
                userInfo = issuedTicket.userInfo,
            )
    }
}
