package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketOptionAnswerAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final IssuedTicketOptionAnswerAdaptor issuedTicketOptionAnswerAdaptor;
    private final EventAdaptor eventAdaptor;

    @RedissonLock(
            LockName = "티켓재고관리",
            paramClassType = TicketItem.class,
            identifier = "id",
            needSameTransaction = true)
    @Transactional
    public void createIssuedTicket(
            TicketItem ticketItem, List<CreateIssuedTicketDTO> createIssuedTicketDTOs) {
        createIssuedTicketDTOs.forEach(
                dto -> {
                    CreateIssuedTicketResponse responseDTO =
                            IssuedTicket.orderLineItemToIssuedTickets(dto);
                    issuedTicketAdaptor.saveAll(responseDTO.getIssuedTickets());
                });
    }

    @RedissonLock(
            LockName = "티켓재고관리",
            paramClassType = TicketItem.class,
            identifier = "id",
            needSameTransaction = true)
    @Transactional
    public void withDrawIssuedTicket(TicketItem ticketItem, List<IssuedTicket> issuedTickets) {
        issuedTickets.forEach(
                issuedTicket -> {
                    issuedTicket.getTicketItem().increaseQuantity(1L);
                    issuedTicketAdaptor.delete(issuedTicket);
                });
    }
}
