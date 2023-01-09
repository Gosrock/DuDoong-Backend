package band.gosrock.domain.domains.order.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class OrderAdaptor {

    private OrderRepository orderRepository;

    public Order save(Order order) {
        return orderRepository.save(order);
    }
}
