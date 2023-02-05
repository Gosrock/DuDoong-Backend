package band.gosrock.api.email.handler;


import band.gosrock.api.email.service.OrderApproveRequestEmailService;
import band.gosrock.api.email.service.OrderMailInfoHelper;
import band.gosrock.domain.common.events.order.CreateOrderEvent;
import band.gosrock.domain.domains.coupon.service.UseCouponService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderHandler {

    private final OrderApproveRequestEmailService orderApproveRequestEmailService;

    private final OrderMailInfoHelper orderMailInfoHelper;

    @TransactionalEventListener(
            classes = CreateOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderFailEvent(CreateOrderEvent createOrderEvent) {
        log.info(createOrderEvent.getOrderUuid() + "주문 생성 이메일 요청");

        if(createOrderEvent.getOrderMethod().isPayment()){
            return;
        }


        orderApproveRequestEmailService.execute(orderMailInfoHelper.execute(
            createOrderEvent.getOrderUuid()));
    }
}
