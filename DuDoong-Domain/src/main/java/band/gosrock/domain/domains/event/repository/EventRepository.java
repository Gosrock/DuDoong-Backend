package band.gosrock.domain.domains.event.repository;


import band.gosrock.domain.domains.event.domain.Event;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Long> {
    List<Event> findAll();

    List<Event> findAllByHostId(Long hostId);

    List<Event> findAllByIdIn(List<Long> ids);
}
