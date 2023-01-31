package band.gosrock.api.event.service;


import band.gosrock.api.event.model.dto.response.EventDetailResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadEventDetailUseCase {
    private final EventAdaptor eventAdaptor;
    private final EventMapper eventMapper;
    private final HostAdaptor hostAdaptor;

    public EventDetailResponse execute(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        return eventMapper.toEventDetailResponse(host, event);
    }
}
