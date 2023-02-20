package band.gosrock.domain.domains.order.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.exception.OrderNotFoundException;
import band.gosrock.domain.domains.order.repository.OrderRepository;
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition;
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@Adaptor
@RequiredArgsConstructor
public class OrderAdaptor {

    private final OrderRepository orderRepository;

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Order findById(Long orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> OrderNotFoundException.EXCEPTION);
    }

    public List<Order> findByEventId(Long eventId) {
        return orderRepository.findByEventId(eventId);
    }

    public Order findByOrderUuid(String uuid) {
        return orderRepository
                .findByOrderUuid(uuid)
                .orElseThrow(() -> OrderNotFoundException.EXCEPTION);
    }

    public List<Order> findByUuidIn(List<String> orderUuids) {
        return orderRepository.findByUuidIn(orderUuids);
    }

    public Optional<Order> findRecentOrderByUserId(Long userId) {
        return orderRepository.findRecentOrder(userId);
    }

    public Slice<Order> findMyOrders(FindMyPageOrderCondition condition, Pageable pageable) {
        return orderRepository.findMyOrders(condition, pageable);
    }

    public Page<Order> findEventOrders(FindEventOrdersCondition condition, Pageable pageable) {
        return orderRepository.findEventOrders(condition, pageable);
    }
}
