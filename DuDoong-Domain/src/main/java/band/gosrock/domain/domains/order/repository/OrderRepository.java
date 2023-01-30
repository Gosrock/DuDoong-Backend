package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long>, OrderCustomRepository {

    Optional<Order> findByUuid(String uuid);

    Optional<Order> findFirstByUserIdOrderByIdDesc(Long userId);
}
