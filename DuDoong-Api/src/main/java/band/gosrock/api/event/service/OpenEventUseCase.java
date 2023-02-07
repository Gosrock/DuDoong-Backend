package band.gosrock.api.event.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.CannotOpenEventException;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class OpenEventUseCase {
    private final EventService eventService;
    private final EventAdaptor eventAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public EventResponse execute(Long eventId) {
        final Event event = eventAdaptor.findById(eventId);
        final Boolean hasBasic = event.hasEventBasic() && event.hasEventPlace();
        final Boolean hasDetail = event.hasEventDetail();
        final Boolean hasTicketItem = ticketItemAdaptor.existsByEventId(event.getId());
        if (hasBasic && hasDetail && hasTicketItem) {
            return EventResponse.of(eventService.openEvent(event));
        }
        // 체크리스트를 달성하지 않으면 이벤트를 열 수 없음
        throw CannotOpenEventException.EXCEPTION;
    }
}
