package band.gosrock.domain.domains.cart.service

import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DoneOrderEventHandler(private val cartAdaptor: CartAdaptor) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(classes = [DoneOrderEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleDoneOrderEvent(doneOrderEvent: DoneOrderEvent) {
        log.info("${doneOrderEvent.orderUuid} 주문 상태 완료, 장바구니를 제거합니다.")
        val userId = doneOrderEvent.userId
        cartAdaptor.deleteByUserId(userId)
    }
}
