package band.gosrock.domain.domains.issuedTicket.service.handlers

import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OrderEventHandler(
    private val issuedTicketDomainService: IssuedTicketDomainService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(classes = [DoneOrderEvent::class])
    fun handleDoneOrderEvent(doneOrderEvent: DoneOrderEvent) {
        log.info("${doneOrderEvent.orderUuid}주문 상태 완료, 티켓 생성작업 진행")
        issuedTicketDomainService.createIssuedTicket(
            doneOrderEvent.itemId,
            doneOrderEvent.orderUuid,
            doneOrderEvent.userId,
        )
        log.info("${doneOrderEvent.orderUuid}주문 상태 완료, 티켓 생성작업 완료")
    }
}
