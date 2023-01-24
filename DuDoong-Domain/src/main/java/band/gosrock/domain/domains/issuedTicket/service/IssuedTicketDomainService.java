package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.exception.HostNotAuthEventException;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketOptionAnswerAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import java.util.Objects;
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
    private final UserAdaptor userAdaptor;
    private final OrderAdaptor orderAdaptor;

    @RedissonLock(LockName = "티켓재고관리", identifier = "itemId")
    @Transactional
    public void createIssuedTicket(
            Long itemId, List<CreateIssuedTicketDTO> createIssuedTicketDTOs) {
        createIssuedTicketDTOs.forEach(
                dto -> {
                    CreateIssuedTicketResponse responseDTO =
                            IssuedTicket.orderLineItemToIssuedTickets(dto);
                    issuedTicketAdaptor.saveAll(responseDTO.getIssuedTickets());
                });
    }

    @RedissonLock(LockName = "티켓재고관리", paramClassType = TicketItem.class, identifier = "id")
    @Transactional
    public void withDrawIssuedTicket(TicketItem ticketItem, List<IssuedTicket> issuedTickets) {
        issuedTickets.forEach(
                issuedTicket -> {
                    issuedTicket.getTicketItem().increaseQuantity(1L);
                    issuedTicketAdaptor.cancel(issuedTicket);
                });
    }

    @Transactional
    public IssuedTicketInfoVo processingEntranceIssuedTicket(
            Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.find(issuedTicketId);
        if (!Objects.equals(issuedTicket.getEvent().getHostId(), currentUserId)) {
            throw HostNotAuthEventException.EXCEPTION;
        }
        issuedTicket.entrance();
        return issuedTicket.toIssuedTicketInfoVo();
    }

    @RedissonLock(LockName = "티켓재고관리", identifier = "itemId")
    public void createIssuedTicket(Long itemId, String orderUuid, Long userId) {
        User user = userAdaptor.queryUser(userId);
        Order order = orderAdaptor.findByOrderUuid(orderUuid);

        List<CreateIssuedTicketDTO> createIssuedTicketDTOS =
            order.getOrderLineItems().stream()
                .map(orderLineItem -> new CreateIssuedTicketDTO(order, orderLineItem, user))
                .toList();

        createIssuedTicketDTOS.forEach(
            dto -> {
                CreateIssuedTicketResponse responseDTO =
                    IssuedTicket.orderLineItemToIssuedTickets(dto);
                issuedTicketAdaptor.saveAll(responseDTO.getIssuedTickets());
            });
    }
}
