package band.gosrock.api.alimTalk.handler

import band.gosrock.api.alimTalk.service.SendDoneOrderAlimTalkService
import band.gosrock.api.alimTalk.service.helper.OrderAlimTalkInfoHelper
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DoneOrderEventAlimTalkHandler(
    private val sendDoneOrderAlimTalkService: SendDoneOrderAlimTalkService,
    private val orderAlimTalkInfoHelper: OrderAlimTalkInfoHelper,
    private val orderAdaptor: OrderAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
) {
    private val log = LoggerFactory.getLogger(DoneOrderEventAlimTalkHandler::class.java)

    @Async
    @TransactionalEventListener(classes = [DoneOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleDoneOrderEvent(doneOrderEvent: DoneOrderEvent) {
        log.info("${doneOrderEvent.orderUuid}주문 상태 완료, 파트너의 공연이면 알림톡 전송")
        // 파트너인 호스트의 공연일 경우만 알림톡 전송
        val order = orderAdaptor.findByOrderUuid(doneOrderEvent.orderUuid)
        val event = eventAdaptor.findById(order.eventId!!)
        val host = hostAdaptor.findById(event.hostId!!)
        if (host.isPartnerHost()) {
            val orderAlimTalkDto = orderAlimTalkInfoHelper.execute(order, event, host)
            sendDoneOrderAlimTalkService.execute(orderAlimTalkDto)
            log.info("${doneOrderEvent.orderUuid}주문 상태 완료, 파트너의 공연 알림톡 전송 완료")
        }
    }
}
