package band.gosrock.domain.domains.event.service;

import static band.gosrock.domain.domains.event.domain.QEvent.event;

import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.*;
import band.gosrock.domain.domains.event.exception.CannotDeleteByIssuedTicketException;
import band.gosrock.domain.domains.event.exception.CannotOpenEventException;
import band.gosrock.domain.domains.event.exception.UseOtherApiException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.ticket_item.service.TicketItemService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventAdaptor eventAdaptor;
    private final TicketItemService ticketItemService;
    private final IssuedTicketAdaptor issuedTicketAdaptor;

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

    public void validateEventBasicExistence(Event event) {
        if (!event.hasEventBasic() || !event.hasEventPlace())
            throw CannotOpenEventException.EXCEPTION;
    }

    public void validateEventDetailExistence(Event event) {
        if (!event.hasEventDetail()) throw CannotOpenEventException.EXCEPTION;
    }

    public Event openEvent(Event event) {
        this.validateEventBasicExistence(event);
        this.validateEventDetailExistence(event);
        ticketItemService.validateExistenceByEventId(event.getId());
        event.open();
        return eventRepository.save(event);
    }

    public Event updateEventStatus(Event event, EventStatus status) {
        if (status == EventStatus.CLOSED) event.close();
        else if (status == EventStatus.CALCULATING) event.calculate();
        else if (status == EventStatus.PREPARING) event.prepare();
        else throw UseOtherApiException.EXCEPTION; // open, deleteSoft 는 다른 API 강제
        return eventRepository.save(event);
    }

    public List<Event> closeExpiredEventsEndAtBefore(LocalDateTime time) {
        List<Event> events = eventAdaptor.queryEventsByEndAtBefore(time);
        events.forEach(event -> updateEventStatus(event, EventStatus.CLOSED));
        eventRepository.saveAll(events);
        return events;
    }

    public Event deleteEventSoft(Event event) {
        // 발급된 티켓이 있다면 삭제 불가
        if (issuedTicketAdaptor.existsByEventId(event.getId()))
            throw CannotDeleteByIssuedTicketException.EXCEPTION;
        event.deleteSoft();
        return eventRepository.save(event);
    }
}
