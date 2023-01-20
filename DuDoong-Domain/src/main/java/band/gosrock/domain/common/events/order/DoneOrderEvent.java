package band.gosrock.domain.common.events.order;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DoneOrderEvent extends DomainEvent {

    private final String orderUuid;

    private final Long userId;
    public static DoneOrderEvent of(String uuid,Long userId) {
        return new DoneOrderEvent(uuid,userId);
    }
}
