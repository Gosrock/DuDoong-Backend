package band.gosrock.api.email.handler

import band.gosrock.api.email.service.OrderMailInfoHelper
import band.gosrock.api.email.service.OrderWithDrawCancelEmailService
import band.gosrock.api.email.service.OrderWithDrawRefundEmailService
import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.order.domain.OrderStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WithDrawOrderEventEmailHandler(
    private val orderWithDrawRefundEmailService: OrderWithDrawRefundEmailService,
    private val orderWithDrawCancelEmailService: OrderWithDrawCancelEmailService,
    private val orderMailInfoHelper: OrderMailInfoHelper,
) {
    @Async
    @TransactionalEventListener(
        classes = [WithDrawOrderEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handleWithDrawOrderEvent(withDrawOrderEvent: WithDrawOrderEvent) {
        val orderStatus = withDrawOrderEvent.orderStatus
        val orderMailDto = orderMailInfoHelper.execute(withDrawOrderEvent.orderUuid)

        // 관리자에 의한 취소
        if (orderStatus == OrderStatus.CANCELED) {
            orderWithDrawCancelEmailService.execute(orderMailDto)
            return
        }
        // 구매자에의한 환불 요청
        orderWithDrawRefundEmailService.execute(orderMailDto)
    }
}
