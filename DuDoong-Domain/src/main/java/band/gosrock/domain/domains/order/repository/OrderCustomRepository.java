package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition;
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface OrderCustomRepository {

    Optional<Order> findByOrderUuid(String orderUuid);

    Slice<Order> findMyOrders(FindMyPageOrderCondition condition, Pageable pageable);

    Page<Order> findEventOrders(FindEventOrdersCondition condition, Pageable pageable);

    Optional<Order> findRecentOrder(Long userId);

}
