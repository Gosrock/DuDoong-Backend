package band.gosrock.domain.common.events.order;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.lang.Nullable;

@Getter
@Builder
@ToString
public class WithDrawOrderEvent extends DomainEvent {

    private final String orderUuid;
    private final Long userId;
    private final OrderMethod orderMethod;
    private final OrderStatus orderStatus;

    private final Boolean isDudoongTicketOrder;
    private final Boolean isRefund;

    @Nullable private final String paymentKey;
    private final Long itemId;

    private final Boolean isUsingCoupon;
    @Nullable private final Long issuedCouponId;

    public static WithDrawOrderEvent from(Order order) {
        return WithDrawOrderEvent.builder()
                .orderMethod(order.getOrderMethod())
                .paymentKey(order.isNeedPaid() ? order.getPaymentKey() : null)
                .userId(order.getUserId())
                .orderUuid(order.getUuid())
                .orderStatus(order.getOrderStatus())
                .itemId(order.getItemId())
                .isUsingCoupon(order.hasCoupon())
                .issuedCouponId(order.getOrderCouponVo().getCouponId())
                .isDudoongTicketOrder(order.isDudoongTicketOrder())
                .isRefund(order.getOrderStatus() == OrderStatus.REFUND)
                .build();
    }
}
