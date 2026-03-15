package band.gosrock.api.email.handler

import band.gosrock.api.email.service.OrderApproveConfirmEmailService
import band.gosrock.api.email.service.OrderMailInfoHelper
import band.gosrock.api.email.service.OrderPaymentDoneEmailService
import band.gosrock.domain.common.events.order.DoneOrderEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(DoneOrderEventEmailHandler::class.java)

@Component
class DoneOrderEventEmailHandler(
    private val orderMailInfoHelper: OrderMailInfoHelper,
    private val orderApproveConfirmEmailService: OrderApproveConfirmEmailService,
    private val orderPaymentDoneEmailService: OrderPaymentDoneEmailService,
) {
    @Async
    @TransactionalEventListener(
        classes = [DoneOrderEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handleDoneOrderEvent(doneOrderEvent: DoneOrderEvent) {
        log.info("${doneOrderEvent.orderUuid}주문 상태 완료, 이메일 보내기")
        val orderMailDto = orderMailInfoHelper.execute(doneOrderEvent.orderUuid)

        // 결제용
        if (doneOrderEvent.orderMethod.isPayment()) {
            orderPaymentDoneEmailService.execute(orderMailDto)
            return
        }
        // 승인용
        orderApproveConfirmEmailService.execute(orderMailDto)
        log.info("${doneOrderEvent.orderUuid}주문 상태 완료, 이메일 보내기 완료")
    }
}
