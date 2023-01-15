package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
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

    public OrderResponse execute(String orderUuid) {
        // TODO : 권한 체크 ( 호스트 관리자인지 )
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String confirmOrderUuid = orderApproveService.execute(orderUuid, currentUserId);
        return orderMapper.toOrderResponse(confirmOrderUuid);
    }
}
