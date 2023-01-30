package band.gosrock.api.order.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
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
    public OrderResponse execute(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        orderValidator.validOwner(order,userUtils.getCurrentUserId());
        return orderMapper.toOrderResponse(order);
    }
}
