package band.gosrock.api.order.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.order.model.dto.response.OrderBriefElement;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadOrderUseCase {
    private final OrderMapper orderMapper;
    private final OrderAdaptor orderAdaptor;

    private final OrderValidator orderValidator;

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
}
