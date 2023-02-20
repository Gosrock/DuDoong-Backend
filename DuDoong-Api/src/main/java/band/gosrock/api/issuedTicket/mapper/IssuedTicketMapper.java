package band.gosrock.api.issuedTicket.mapper;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.issuedTicket.dto.response.IssuedTicketAdminTableElement;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDTO;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condition.IssuedTicketCondition;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class IssuedTicketMapper {

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    private final EventAdaptor eventAdaptor;

    private final UserAdaptor userAdaptor;
    private final OrderAdaptor orderAdaptor;

    @Transactional(readOnly = true)
    public PageResponse<IssuedTicketAdminTableElement> toIssuedTicketAdminTableElementPageResponse(
        Pageable page, IssuedTicketCondition condition) {
        Page<IssuedTicket> issuedTickets = issuedTicketAdaptor.searchIssuedTicket(page, condition);
        List<String> orderUuids = issuedTickets.stream().map(IssuedTicket::getOrderUuid).distinct().toList();
        List<Long> userIds = issuedTickets.stream().map(IssuedTicket::getUserId).distinct().toList();
        List<User> users = userAdaptor.findUserByIdIn(userIds);
        List<Order> orders = orderAdaptor.findByUuidIn(orderUuids);
        Page<IssuedTicketAdminTableElement> issuedTicketAdminTableElements = issuedTickets.map(issuedTicket -> IssuedTicketAdminTableElement.of(issuedTicket, getUser(users, issuedTicket.getUserId()), getOrder(orders, issuedTicket.getOrderUuid())));
        return PageResponse.of(issuedTicketAdminTableElements);
    }

    @NotNull
    private static Order getOrder(List<Order> orders, String orderUuid) {
        return orders.stream().filter(o -> o.getUuid().equals(orderUuid))
            .findFirst().orElseThrow();
    }

    @NotNull
    private static User getUser(List<User> users, Long userId) {
        return users.stream().filter(u -> u.getId().equals(userId)).findFirst()
            .orElseThrow();
    }

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketDetailResponse toIssuedTicketDetailResponse(
            Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.findForUser(currentUserId, issuedTicketId);
        Event event = eventAdaptor.findById(issuedTicket.getEventId());
        return RetrieveIssuedTicketDetailResponse.of(issuedTicket, event);
    }

    @Transactional(readOnly = true)
    public IssuedTicket getIssuedTicket(Long issuedTicketId) {
        return issuedTicketAdaptor.queryIssuedTicket(issuedTicketId);
    }
}
