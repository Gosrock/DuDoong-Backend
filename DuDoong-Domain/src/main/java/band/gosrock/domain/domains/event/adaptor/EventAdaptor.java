package band.gosrock.domain.domains.event.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.EventNotFoundException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
public class EventAdaptor {

    private final EventRepository eventRepository;

    public Event findById(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> EventNotFoundException.EXCEPTION);
    }

    // Todo:: hostId 지정해서 뽑아오도록 변경하기 (임시)
    public List<Event> findAllByHostId(Long hostId) {
        return eventRepository.findAll();
    }

}
