package band.gosrock.domain.common.events.order;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
@Builder
public class CreateOrderEvent extends DomainEvent {

    private final String orderUuid;

    private final Long userId;

    private final Boolean isUsingCoupon;

    @Nullable private final Long issuedCouponId;

    public static CreateOrderEvent from(Order order) {

        return CreateOrderEvent.builder()
                .userId(order.getUserId())
                .orderUuid(order.getUuid())
                .isUsingCoupon(order.hasCoupon())
                .issuedCouponId(order.getOrderCouponVo().getCouponId())
                .build();
    }
}
