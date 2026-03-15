package band.gosrock.api.alimTalk.service.helper

import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto
import band.gosrock.common.annotation.Helper
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkEventInfo
import org.springframework.transaction.annotation.Transactional

@Helper
@Transactional(readOnly = true)
class OrderAlimTalkInfoHelper(
    private val userAdaptor: UserAdaptor,
) {
    fun execute(order: Order, event: Event, host: Host): OrderAlimTalkDto {
        val user = userAdaptor.queryUser(order.userId)
        return OrderAlimTalkDto(
            userInfo = user.toAlimTalkUserInfo(),
            orderInfo = order.toAlimTalkOrderInfo(),
            eventInfo = getEventInfo(event, host),
        )
    }

    private fun getEventInfo(event: Event, host: Host): AlimTalkEventInfo =
        AlimTalkEventInfo(host.profile!!.name!!, event.eventBasic!!.name!!)
}
