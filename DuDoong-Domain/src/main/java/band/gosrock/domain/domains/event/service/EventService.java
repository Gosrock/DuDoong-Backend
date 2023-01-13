package band.gosrock.domain.domains.event.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.HostNotAuthEventException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventAdaptor eventAdaptor;

    public void checkEventHost(Long hostId, Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        if (!Objects.equals(event.getHostId(), hostId)) {
            throw HostNotAuthEventException.EXCEPTION;
        }
    }
}
