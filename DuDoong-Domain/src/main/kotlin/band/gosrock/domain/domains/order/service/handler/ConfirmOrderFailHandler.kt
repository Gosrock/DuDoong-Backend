package band.gosrock.domain.domains.order.service.handler

import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.coupon.service.RecoveryCouponService
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.service.WithdrawPaymentService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ConfirmOrderFailHandler(
    private val cancelPaymentService: WithdrawPaymentService,
    private val issuedTicketDomainService: IssuedTicketDomainService,
    private val recoveryCouponService: RecoveryCouponService,
    private val orderAdaptor: OrderAdaptor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(classes = [DoneOrderEvent::class], phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleDoneOrderFailEvent(doneOrderEvent: DoneOrderEvent) {
        log.info("${doneOrderEvent.orderUuid} 주문 실패 처리 핸들러")
        val order = orderAdaptor.findByOrderUuid(doneOrderEvent.orderUuid)
        order.fail()

        if (order.hasCoupon()) {
            recoveryCouponService.execute(order.userId!!, order.orderCouponVo.couponId)
        }

        issuedTicketDomainService.doneOrderEventAfterRollBackWithdrawIssuedTickets(
            doneOrderEvent.itemId, doneOrderEvent.orderUuid
        )

        if (order.isNeedPaid()) {
            log.info("${doneOrderEvent.orderUuid}:${doneOrderEvent.paymentKey} 주문 실패 시 결제 취소")
            cancelPaymentService.execute(order.uuid!!, doneOrderEvent.paymentKey!!, "서버 오류로 인한 환불")
        }
    }
}
