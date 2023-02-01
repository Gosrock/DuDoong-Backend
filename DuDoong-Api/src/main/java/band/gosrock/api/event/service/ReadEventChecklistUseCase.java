package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.event.model.dto.response.EventChecklistResponse;
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
public class ReadEventChecklistUseCase {
    private final UserUtils userUtils;
    private final EventAdaptor eventAdaptor;
    private final EventMapper eventMapper;
    private final HostAdaptor hostAdaptor;

    public EventChecklistResponse execute(Long eventId) {
        final Event event = eventAdaptor.findById(eventId);
        final Host host = hostAdaptor.findById(event.getHostId());
        final Long userId = userUtils.getCurrentUserId();
        host.validateHostUser(userId);

        return eventMapper.toEventChecklistResponse(event);
    }
}
