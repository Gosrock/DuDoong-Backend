package band.gosrock.domain.domains.issuedTicket.service.handlers

import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WithdrawOrderEventHandler(
    private val issuedTicketDomainService: IssuedTicketDomainService,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(
        classes = [WithDrawOrderEvent::class],
        phase = TransactionPhase.BEFORE_COMMIT,
    )
    fun handleWithdrawOrderEvent(withDrawOrderEvent: WithDrawOrderEvent) {
        log.info("${withDrawOrderEvent.orderUuid}주문 상태 철회 , 티켓 철회 필요")
        val issuedTickets = issuedTicketAdaptor.findAllByOrderUuid(withDrawOrderEvent.orderUuid)
        issuedTicketDomainService.withdrawIssuedTicket(
            withDrawOrderEvent.itemId, issuedTickets,
        )
    }
}
