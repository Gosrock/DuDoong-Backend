package band.gosrock.domain.domains.order.service.handler;


import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.domains.coupon.service.RecoveryCouponService;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.service.WithdrawPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmOrderFailHandler {

    private final WithdrawPaymentService cancelPaymentService;
    private final IssuedTicketDomainService issuedTicketDomainService;
    private final RecoveryCouponService recoveryCouponService;

    private final OrderAdaptor orderAdaptor;
    @Async
    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional
    public void handleDoneOrderFailEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getOrderUuid() + "주문 실패 처리 핸들러");

        Order order = orderAdaptor.findByOrderUuid(doneOrderEvent.getOrderUuid());
        order.fail();

        if (order.hasCoupon()) { // 쿠폰 사용했을 시 쿠폰 복구
            recoveryCouponService.execute(
                    order.getUserId(), order.getOrderCouponVo().getCouponId());
        }

        issuedTicketDomainService.doneOrderEventAfterRollBackWithdrawIssuedTickets(
                doneOrderEvent.getItemId(), doneOrderEvent.getOrderUuid());

        if (order.isNeedPaid()) {
            log.info(
                    doneOrderEvent.getOrderUuid()
                            + ":"
                            + doneOrderEvent.getPaymentKey()
                            + "주문 실패 시 결제 취소");
            cancelPaymentService.execute(
                    order.getUuid(), doneOrderEvent.getPaymentKey(), "서버 오류로 인한 환불");
        }
    }
}
