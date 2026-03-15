package band.gosrock.api.email.service

import band.gosrock.api.email.dto.IssuedTicketMailDTO
import band.gosrock.common.annotation.Helper
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo
import org.springframework.transaction.annotation.Transactional

@Helper
@Transactional(readOnly = true)
class IssuedTicketMailInfoHelper(
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val hostAdaptor: HostAdaptor,
) {
    fun execute(issuedTicketNo: String): IssuedTicketMailDTO {
        val issuedTicket = issuedTicketAdaptor.queryByIssuedTicketNo(issuedTicketNo)
        val user = userAdaptor.queryUser(issuedTicket.userInfo?.userId)
        val event = eventAdaptor.findById(issuedTicket.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        return IssuedTicketMailDTO(
            user.toEmailUserInfo(),
            issuedTicket.toEmailIssuedTicketInfo(),
            getEventInfo(event, host),
        )
    }

    private fun getEventInfo(event: Event, host: Host): EmailEventInfo {
        return EmailEventInfo(host.profile!!.name!!, event.eventBasic!!.name!!)
    }
}
