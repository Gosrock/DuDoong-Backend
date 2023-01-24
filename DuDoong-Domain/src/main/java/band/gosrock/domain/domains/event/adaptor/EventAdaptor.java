package band.gosrock.domain.domains.event.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.EventNotFoundException;
import band.gosrock.domain.domains.event.repository.EventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class EventAdaptor {

    private final EventRepository eventRepository;

    public Event findById(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> EventNotFoundException.EXCEPTION);
    }

    /** url 표시 이름 중복 확인하는 쿼리 */
    public Boolean existByAliasUrl(String urlName) {
        return eventRepository.existsByUrlName(urlName);
    }

    public List<Event> findAllByHostId(Long hostId) {
        return eventRepository.findAllByHostId(hostId);
    }
}
