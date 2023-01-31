package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.repository.condition.FindEventOrdersCondition;
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomRepository {

    Page<Order> findMyOrders(FindMyPageOrderCondition condition, Pageable pageable);

    Page<Order> findEventOrders(FindEventOrdersCondition condition, Pageable pageable);
}
