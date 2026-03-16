package band.gosrock.domain.domains.order.service.handler

import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.service.WithdrawPaymentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WithDrawOrderHandler(
    private val withdrawPaymentService: WithdrawPaymentService,
    private val orderAdaptor: OrderAdaptor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleWithDrawOrderEvent(withDrawOrderEvent: WithDrawOrderEvent) {
        log.info("${withDrawOrderEvent.orderUuid} 주문 철회 핸들러")
        val orderStatus = withDrawOrderEvent.orderStatus
        val order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.orderUuid)
        if (!order.isPaid()) return

        val reason = when (orderStatus) {
            OrderStatus.CANCELED -> "이벤트 관리자에 의한 취소"
            OrderStatus.REFUND -> "구매자에의한 환불 요청"
            else -> "결제 취소"
        }

        log.info("${withDrawOrderEvent.orderUuid} 주문 철회 ${order.paymentKey}$reason")
        withdrawPaymentService.execute(withDrawOrderEvent.orderUuid, order.paymentKey, reason)
    }
}
