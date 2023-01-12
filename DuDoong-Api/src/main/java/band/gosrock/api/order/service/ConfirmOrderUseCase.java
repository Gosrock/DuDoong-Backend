package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.mapper.OrderMapper;
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.OrderConfirmService;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ConfirmOrderUseCase {

    private final OrderConfirmService orderConfirmService;

    private final OrderMapper orderMapper;

    public OrderResponse execute(String orderUuid, ConfirmOrderRequest confirmOrderRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ConfirmPaymentsRequest confirmPaymentsRequest =
                ConfirmPaymentsRequest.builder()
                        .paymentKey(confirmOrderRequest.getPaymentKey())
                        .amount(confirmOrderRequest.getAmount())
                        .orderId(orderUuid)
                        .build();
        String confirmOrderUuid =
                orderConfirmService.execute(confirmPaymentsRequest, currentUserId);
        return orderMapper.toOrderResponse(confirmOrderUuid);
    }
}
