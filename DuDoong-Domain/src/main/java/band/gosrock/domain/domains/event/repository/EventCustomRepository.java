package band.gosrock.domain.domains.event.repository;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface EventCustomRepository {
    Slice<Event> querySliceEventsByHostIdIn(List<Long> hostId, Pageable pageable);

    Slice<Event> querySliceEventsByStatus(EventStatus status, Pageable pageable);
}
