package band.gosrock.domain.domains.event.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.EventNotFoundException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Adaptor
@RequiredArgsConstructor
public class EventAdaptor {

    private final EventRepository eventRepository;

    public Event findById(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> EventNotFoundException.EXCEPTION);
    }

    public Page<Event> findAllByHostId(Long hostId, Pageable pageable) {
        return eventRepository.findAllByHostId(hostId, pageable);
    }

    public Page<Event> findAllByHostIdIn(List<Long> hostId, Pageable pageable) {
        return eventRepository.findAllByHostIdIn(hostId, pageable);
    }

    public List<Event> findAllByIds(List<Long> ids) {
        return eventRepository.findAllByIdIn(ids);
    }
}
