package band.gosrock.domain.common.events.order;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WithDrawOrderEvent extends DomainEvent {

    private final String uuid;
    private final Long userId;
    public static WithDrawOrderEvent of(String uuid,Long userId) {
        return new WithDrawOrderEvent(uuid,userId);
    }
}
