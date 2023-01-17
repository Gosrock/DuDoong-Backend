package band.gosrock.api.issuedTicket.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketForDevDTO;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final UserAdaptor userAdaptor;
    private final EventAdaptor eventAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;
    private final OptionAdaptor optionAdaptor;

    @Transactional
    public RetrieveIssuedTicketDetailResponse executeForDev(CreateIssuedTicketForDevDTO body) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = getUser(currentUserId);
        Event event = getEvent(body.getEventId());
        TicketItem ticketItem = getTicketItem(body.getTicketItemId());

        List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers =
                body.getOptionAnswers().stream()
                        .map(
                                option ->
                                        IssuedTicketOptionAnswer.builder()
                                                .option(optionAdaptor.find(option))
                                                .answer("test")
                                                .build())
                        .toList();

        IssuedTicket issuedTicket =
                issuedTicketAdaptor.save(
                        IssuedTicket.createForDev(
                                event,
                                user,
                                body.getOrderLineId(),
                                ticketItem,
                                Money.wons(body.getAmount()),
                                issuedTicketOptionAnswers));

        return new RetrieveIssuedTicketDetailResponse(
                issuedTicket.toIssuedTicketInfoVo(),
                issuedTicket.getEvent().toEventInfoVo(),
                issuedTicket.getUser().getProfile().getName());
    }

    private Event getEvent(Long eventId) {
        return eventAdaptor.findById(eventId);
    }

    private User getUser(Long userId) {
        return userAdaptor.queryUser(userId);
    }

    private TicketItem getTicketItem(Long ticketItemId) {
        return ticketItemAdaptor.find(ticketItemId);
    }
}
