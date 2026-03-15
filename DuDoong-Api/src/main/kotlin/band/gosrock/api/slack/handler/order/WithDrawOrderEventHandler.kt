package band.gosrock.api.slack.handler.order

import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
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
class WithDrawOrderEventHandler(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    private val log = LoggerFactory.getLogger(WithDrawOrderEventHandler::class.java)

    @Async
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    fun handle(withDrawOrderEvent: WithDrawOrderEvent) {
        log.info("선착순 유료,무료  승인 무료 시에 전송되는 알람")
        if (withDrawOrderEvent.isDudoongTicketOrder) return

        val order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.orderUuid)
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        val message = HostSlackAlarm.withDrawOrder(event, order)
        slackMessageProvider.sendMessage(host.slackUrl!!, message)
    }
}
