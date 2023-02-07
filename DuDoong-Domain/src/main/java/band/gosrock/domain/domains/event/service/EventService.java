package band.gosrock.domain.domains.event.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.*;
import band.gosrock.domain.domains.event.exception.UseOtherApiException;
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

    public Event openEvent(Event event) {
        event.open();
        return eventRepository.save(event);
    }

    public Event updateEventStatus(Event event, EventStatus status) {
        if (status == EventStatus.OPEN) {
            throw UseOtherApiException.EXCEPTION; // open 은 다른 API 강제
        }
        // todo :: 이벤트 상태 변경시 검증 필요
        else if (status == EventStatus.CLOSED) event.close();
        else if (status == EventStatus.CALCULATING) event.calculate();
        else if (status == EventStatus.PREPARING) event.prepare();
        return eventRepository.save(event);
    }
}
