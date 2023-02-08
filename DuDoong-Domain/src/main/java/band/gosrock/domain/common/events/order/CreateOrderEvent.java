package band.gosrock.domain.common.events.order;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.lang.Nullable;

@Getter
@Builder
@ToString
public class CreateOrderEvent extends DomainEvent {

    private final String orderUuid;

    private final Long userId;

    private final Boolean isUsingCoupon;
    private final OrderMethod orderMethod;

    @Nullable private final Long issuedCouponId;

    public static CreateOrderEvent from(Order order) {

        return CreateOrderEvent.builder()
                .userId(order.getUserId())
                .orderUuid(order.getUuid())
                .isUsingCoupon(order.hasCoupon())
                .issuedCouponId(order.getOrderCouponVo().getCouponId())
                .orderMethod(order.getOrderMethod())
                .build();
    }
}
