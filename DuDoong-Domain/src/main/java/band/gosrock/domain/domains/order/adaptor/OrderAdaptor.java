package band.gosrock.domain.domains.order.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.exception.OrderNotFoundException;
import band.gosrock.domain.domains.order.repository.OrderRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    public Order findByOrderUuid(String uuid) {
        return orderRepository.findByUuid(uuid).orElseThrow(() -> OrderNotFoundException.EXCEPTION);
    }

    public Optional<Order> findRecentOrderByUserId(Long userId) {
        return orderRepository.findFirstByUserIdOrderByIdDesc(userId);
    }

    public Page<Order> findOrdersWithPagination(Long userId, Pageable pageable) {
        return orderRepository.getOrdersWithPage(userId, pageable);
    }
}
