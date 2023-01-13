package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.mapper.OrderMapper;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.WithdrawOrderService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final WithdrawOrderService withdrawOrderService;

    private final OrderMapper orderMapper;

    public OrderResponse execute(String orderUuid) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        withdrawOrderService.cancelOrder(orderUuid, currentUserId);
        return orderMapper.toOrderResponse(orderUuid);
    }
}
