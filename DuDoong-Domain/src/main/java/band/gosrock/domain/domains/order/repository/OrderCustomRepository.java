package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomRepository {

    Page<Order> getOrdersWithPage(Long userId, Pageable pageable);
}
