package band.gosrock.api.event.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.event.model.dto.response.EventChecklistResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadEventChecklistUseCase {
    private final EventAdaptor eventAdaptor;
    private final EventMapper eventMapper;

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    public EventChecklistResponse execute(Long eventId) {
        final Event event = eventAdaptor.findById(eventId);
        return eventMapper.toEventChecklistResponse(event);
    }
}
