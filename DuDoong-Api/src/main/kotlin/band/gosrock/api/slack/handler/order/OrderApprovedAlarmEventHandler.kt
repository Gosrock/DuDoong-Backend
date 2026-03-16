package band.gosrock.api.slack.handler.order

import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderApprovedAlarmEventHandler(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    private val log = LoggerFactory.getLogger(OrderApprovedAlarmEventHandler::class.java)

    @Async
    @TransactionalEventListener(classes = [DoneOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handle(doneOrderEvent: DoneOrderEvent) {
        if (doneOrderEvent.orderMethod.isPayment()) return
        log.info("승인 방식 완료 시에 알림 전송")
        val order = orderAdaptor.findByOrderUuid(doneOrderEvent.orderUuid)
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        val message = HostSlackAlarm.approvedOrder(event, order)
        slackMessageProvider.sendMessage(host.slackUrl!!, message)
    }
}
