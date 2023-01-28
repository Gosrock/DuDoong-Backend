package band.gosrock.domain.domains.event.repository;


import band.gosrock.domain.domains.event.domain.Event;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Long> {
    List<Event> findAll();

    Page<Event> findAllByHostId(Long hostId, Pageable pageable);

    List<Event> findAllByIdIn(List<Long> ids);
}
