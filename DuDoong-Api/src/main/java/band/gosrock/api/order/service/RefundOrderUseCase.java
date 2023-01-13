package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.mapper.OrderMapper;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.WithdrawOrderService;
import lombok.RequiredArgsConstructor;

/** 환불을 위함 환불이라 함은, 사용자가 직접 구매한 물품을 취소시킴 */
@UseCase
@RequiredArgsConstructor
public class RefundOrderUseCase {
    private final WithdrawOrderService withdrawOrderService;

    private final OrderMapper orderMapper;

    public OrderResponse execute(String orderUuid) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        withdrawOrderService.refundOrder(orderUuid, currentUserId);
        return orderMapper.toOrderResponse(orderUuid);
    }
}
