package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long>, OrderCustomRepository {

    List<Order> findByEventId(Long eventId);

    List<Order> findByUuidIn(List<String> uuids);
}
