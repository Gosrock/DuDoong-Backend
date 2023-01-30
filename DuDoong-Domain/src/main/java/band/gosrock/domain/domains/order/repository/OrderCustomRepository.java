package band.gosrock.domain.domains.order.repository;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condition.IssuedTicketCondition;
import band.gosrock.domain.domains.order.domain.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomRepository {

    Page<Order> getOrdersWithPage(Long userId , Pageable pageable);
}
