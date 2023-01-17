package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketOptionAnswerAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketForDevDTO;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketRequestForDev;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class IssuedTicketDomainDevService {

    private final EventAdaptor eventAdaptor;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final IssuedTicketOptionAnswerAdaptor issuedTicketOptionAnswerAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;
    private final OptionAdaptor optionAdaptor;

    @Transactional
    public IssuedTicket createIssuedTicket(CreateIssuedTicketForDevDTO createDTO, User user) {
        Event event = eventAdaptor.findById(createDTO.getEventId());
        TicketItem ticketItem = ticketItemAdaptor.find(createDTO.getTicketItemId());

        List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers =
                createDTO.getOptionAnswers().stream()
                        .map(
                                option ->
                                        IssuedTicketOptionAnswer.builder()
                                                .option(optionAdaptor.find(option))
                                                .answer("test")
                                                .build())
                        .toList();

        IssuedTicket issuedTicket =
                IssuedTicket.createForDev(
                        new CreateIssuedTicketRequestForDev(
                                event,
                                createDTO.getOrderLineId(),
                                user,
                                Money.wons(createDTO.getAmount()),
                                ticketItem,
                                issuedTicketOptionAnswers));
        return issuedTicketAdaptor.save(issuedTicket);
    }
}
