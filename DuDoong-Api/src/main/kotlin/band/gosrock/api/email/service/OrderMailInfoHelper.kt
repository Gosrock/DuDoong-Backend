package band.gosrock.api.email.service

import band.gosrock.api.email.dto.OrderMailDto
import band.gosrock.common.annotation.Helper
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo
import org.springframework.transaction.annotation.Transactional

@Helper
@Transactional(readOnly = true)
class OrderMailInfoHelper(
    private val orderAdaptor: OrderAdaptor,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
) {
    fun execute(orderUuid: String): OrderMailDto {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val user = userAdaptor.queryUser(order.userId)
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        return OrderMailDto(
            user.toEmailUserInfo(),
            order.toEmailOrderInfo(),
            getEventInfo(event, host),
        )
    }

    private fun getEventInfo(event: Event, host: Host): EmailEventInfo {
        return EmailEventInfo(host.profile!!.name!!, event.eventBasic!!.name!!)
    }
}
