package band.gosrock.api.email.handler

import band.gosrock.api.email.service.OrderApproveRequestEmailService
import band.gosrock.api.email.service.OrderMailInfoHelper
import band.gosrock.domain.common.events.order.CreateOrderEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(CreateOrderEventEmailHandler::class.java)

@Component
class CreateOrderEventEmailHandler(
    private val orderApproveRequestEmailService: OrderApproveRequestEmailService,
    private val orderMailInfoHelper: OrderMailInfoHelper,
) {
    @Async
    @TransactionalEventListener(
        classes = [CreateOrderEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handleDoneOrderFailEvent(createOrderEvent: CreateOrderEvent) {
        log.info("${createOrderEvent.orderUuid}주문 생성 이메일 요청")

        // 승인 주문만 생성시에 이메일을 보낸다.
        if (createOrderEvent.orderMethod.isPayment()) {
            return
        }

        orderApproveRequestEmailService.execute(
            orderMailInfoHelper.execute(createOrderEvent.orderUuid),
        )
    }
}
