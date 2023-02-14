package band.gosrock.api.order.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.mapper.OrderMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.OrderApproveService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApproveOrderUseCase {

    private final OrderApproveService orderApproveService;

    private final OrderMapper orderMapper;

    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public OrderResponse execute(Long eventId, String orderUuid) {
        String confirmOrderUuid = orderApproveService.execute(orderUuid);
        return orderMapper.toOrderResponse(confirmOrderUuid);
    }
}
