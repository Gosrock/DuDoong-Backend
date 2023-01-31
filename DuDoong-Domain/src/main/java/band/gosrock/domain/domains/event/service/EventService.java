package band.gosrock.domain.domains.event.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.*;
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

    public Event updateEventBasic(Event event, EventBasic eventBasic, EventPlace eventPlace) {
        event.setEventBasic(eventBasic);
        event.setEventPlace(eventPlace);
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

    public Event updateEventStatus(Event event, EventStatus status) {
        // todo :: 이벤트 상태 변경시 검증 필요
        if (status == EventStatus.OPEN) event.open();
        else if (status == EventStatus.CLOSED) event.close();
        else if (status == EventStatus.PREPARING) event.prepare();
        return eventRepository.save(event);
    }

    public void checkEventHost(Long hostId, Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        if (!event.getHostId().equals(hostId)) {
            throw HostNotAuthEventException.EXCEPTION;
        }
    }
}
