package band.gosrock.api.slack.handler.order

import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.order.CreateOrderEvent
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
class NewApproveOrderAlarmEventHandler(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    private val log = LoggerFactory.getLogger(NewApproveOrderAlarmEventHandler::class.java)

    @Async
    @TransactionalEventListener(classes = [CreateOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    fun handle(createOrderEvent: CreateOrderEvent) {
        // 승인 방식의 결제만 알림 발송 대상.
        if (createOrderEvent.orderMethod.isPayment()) return
        log.info("승인 방식 요청 알림 전송")
        val order = orderAdaptor.findByOrderUuid(createOrderEvent.orderUuid)
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        val message = HostSlackAlarm.newApproveOrder(event, order)
        slackMessageProvider.sendMessage(host.slackUrl!!, message)
    }
}
