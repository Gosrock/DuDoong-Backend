package band.gosrock.api.order.service;


import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.FreeOrderService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FreeOrderUseCase {

    private final FreeOrderService freeOrderService;

    private final OrderMapper orderMapper;

    public OrderResponse execute(String orderUuid) {
        String confirmOrderUuid = freeOrderService.execute(orderUuid);
        return orderMapper.toOrderResponse(confirmOrderUuid);
    }
}
