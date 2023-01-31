package band.gosrock.api.order.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.order.model.dto.request.AdminOrderTableQueryRequest;
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement;
import band.gosrock.api.order.model.dto.response.OrderBriefElement;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import band.gosrock.domain.domains.order.repository.condition.FindMyPageOrderCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadOrderUseCase {
    private final OrderMapper orderMapper;
    private final OrderAdaptor orderAdaptor;

    private final OrderValidator orderValidator;
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final UserUtils userUtils;

    public OrderResponse getOrderDetail(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        orderValidator.validOwner(order, userUtils.getCurrentUserId());
        return orderMapper.toOrderResponse(order);
    }

    public OrderBriefElement getRecentOrder() {
        Long currentUserId = userUtils.getCurrentUserId();
        Optional<Order> recentOrder = orderAdaptor.findRecentOrderByUserId(currentUserId);
        return recentOrder.map(orderMapper::toOrderBriefElement).orElse(null);
    }

    public PageResponse<OrderBriefElement> getMyOrders(Boolean showing, Pageable pageable) {
        Long currentUserId = userUtils.getCurrentUserId();

        FindMyPageOrderCondition condition =
                showing == Boolean.TRUE
                        ? FindMyPageOrderCondition.onShowing(currentUserId)
                        : FindMyPageOrderCondition.notShowing(currentUserId);

        Page<Order> ordersWithPagination = orderAdaptor.findMyOrders(condition, pageable);
        Page<OrderBriefElement> orderBriefElements =
                orderMapper.toOrderBriefsResponse(ordersWithPagination);
        return PageResponse.of(orderBriefElements);
    }

    public PageResponse<OrderAdminTableElement> getEventOrders(
            Long eventId,
            AdminOrderTableQueryRequest adminOrderTableQueryRequest,
            Pageable pageable) {

        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        Long userId = userUtils.getCurrentUserId();
        host.validateHostUser(userId);

        Page<Order> orders =
                orderAdaptor.findEventOrders(
                        adminOrderTableQueryRequest.toCondition(eventId), pageable);
        return PageResponse.of(orderMapper.toOrderAdminTableElement(orders));
    }
}
