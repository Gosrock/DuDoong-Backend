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
public class DoneOrderEvent extends DomainEvent {

    private final String orderUuid;

    private final Long userId;

    private final OrderMethod orderMethod;

    @Nullable private final String paymentKey;

    private final Long itemId;

    public static DoneOrderEvent from(Order order) {
        return DoneOrderEvent.builder()
                .orderMethod(order.getOrderMethod())
                .paymentKey(order.isNeedPaid() ? order.getPaymentKey() : null)
                .userId(order.getUserId())
                .orderUuid(order.getUuid())
                .itemId(order.getItemId())
                .build();
    }
}
