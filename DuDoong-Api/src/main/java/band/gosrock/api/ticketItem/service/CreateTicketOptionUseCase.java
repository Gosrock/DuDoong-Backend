package band.gosrock.api.ticketItem.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.service.TicketOptionService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateTicketOptionUseCase {

    private final EventAdaptor eventAdaptor;
    private final TicketOptionMapper ticketOptionMapper;
    private final TicketOptionService ticketOptionService;

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    public OptionGroupResponse execute(
            CreateTicketOptionRequest createTicketOptionRequest, Long eventId) {

        OptionGroup ticketOption =
                ticketOptionMapper
                        .toOptionGroup(createTicketOptionRequest, eventId)
                        .createTicketOption(
                                Money.wons(createTicketOptionRequest.getAdditionalPrice()));
        OptionGroup ticketOptionResult = ticketOptionService.createTicketOption(ticketOption);
        return OptionGroupResponse.from(ticketOptionResult);
    }
}
