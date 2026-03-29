package band.gosrock.api.alimTalk.handler

import band.gosrock.api.alimTalk.service.SendWithdrawOrderAlimTalkService
import band.gosrock.api.alimTalk.service.helper.OrderAlimTalkInfoHelper
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WithDrawOrderEventAlimTalkHandler(
    private val sendWithdrawOrderAlimTalkService: SendWithdrawOrderAlimTalkService,
    private val orderAlimTalkInfoHelper: OrderAlimTalkInfoHelper,
    private val orderAdaptor: OrderAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
) {
    private val log = org.slf4j.LoggerFactory.getLogger(WithDrawOrderEventAlimTalkHandler::class.java)

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleWithDrawOrderEvent(withDrawOrderEvent: WithDrawOrderEvent) {
        try {
            val order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.orderUuid)
            val event = eventAdaptor.findById(order.eventId!!)
            val host = hostAdaptor.findById(event.hostId!!)
            if (host.isPartnerHost()) {
                val orderAlimTalkDto = orderAlimTalkInfoHelper.execute(order, event, host)
                sendWithdrawOrderAlimTalkService.execute(orderAlimTalkDto)
            }
        } catch (e: Exception) {
            log.warn("주문 취소 알림톡 전송 실패 (orderUuid=${withDrawOrderEvent.orderUuid}): ${e.message}")
        }
    }
}
