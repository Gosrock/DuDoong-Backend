package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long>, OrderCustomRepository {

    List<Order> findByEventId(Long eventId);

    List<Order> findByEventIdAndUserIdAndOrderStatus(
            Long eventId, Long userId, OrderStatus orderStatus);

    List<Order> findByUuidIn(List<String> uuids);
}
