package band.gosrock.domain.domains.event.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventDetail;
import band.gosrock.domain.domains.event.domain.EventPlace;
import band.gosrock.domain.domains.event.exception.AlreadyExistEventUrlNameException;
import band.gosrock.domain.domains.event.exception.HostNotAuthEventException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventAdaptor eventAdaptor;

    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    public Event updateEventUrlName(Event event, String urlName) {
        // 중복된 URL 표시 이름 불가
        if (eventAdaptor.existByAliasUrl(urlName)) {
            throw AlreadyExistEventUrlNameException.EXCEPTION;
        }
        event.setUrlName(urlName);
        return eventRepository.save(event);
    }

    public Event updateEventDetail(Event event, EventDetail eventDetail) {
        event.setEventDetail(eventDetail);
        return eventRepository.save(event);
    }

    public Event updateEventPlace(Event event, EventPlace eventPlace) {
        event.setEventPlace(eventPlace);
        return eventRepository.save(event);
    }

    public void checkEventHost(Long hostId, Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        if (!event.getHostId().equals(hostId)) {
            throw HostNotAuthEventException.EXCEPTION;
        }
    }
}
