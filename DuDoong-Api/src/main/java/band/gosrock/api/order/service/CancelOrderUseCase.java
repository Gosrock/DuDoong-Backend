package band.gosrock.api.order.service;


import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.WithdrawOrderService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final WithdrawOrderService withdrawOrderService;

    private final OrderMapper orderMapper;

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID, applyTransaction = false)
    public OrderResponse execute(Long eventId, String orderUuid) {
        withdrawOrderService.cancelOrder(orderUuid);
        return orderMapper.toOrderResponse(orderUuid);
    }
}
