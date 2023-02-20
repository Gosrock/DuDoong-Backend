package band.gosrock.domain.domains.settlement.repository;


import band.gosrock.domain.domains.settlement.domain.EventSettlement;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface EventSettlementRepository extends CrudRepository<EventSettlement, Long> {

    Optional<EventSettlement> findByEventId(Long eventId);
}
