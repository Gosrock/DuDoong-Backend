package band.gosrock.domain.domains.event.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import band.gosrock.domain.domains.event.exception.EventNotFoundException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

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

    public Slice<Event> querySliceEventsByHostIdIn(List<Long> hostId, Pageable pageable) {
        return eventRepository.querySliceEventsByHostIdIn(hostId, pageable);
    }

    public Slice<Event> querySliceEventsByStatus(EventStatus status, Pageable pageable) {
        return eventRepository.querySliceEventsByStatus(status, pageable);
    }

    public Slice<Event> querySliceEventsByKeyword(String keyword, Pageable pageable) {
        return eventRepository.querySliceEventsByKeyword(keyword, pageable);
    }

    public List<Event> queryEventsByEndAtBefore(LocalDateTime time) {
        return eventRepository.queryEventsByEndAtBefore(time);
    }

    public List<Event> findAllByIds(List<Long> ids) {
        return eventRepository.findAllByIdIn(ids);
    }
}
