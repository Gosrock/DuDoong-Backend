package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long>, OrderCustomRepository {}
