package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.service.OrderConfirmService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ConfirmOrderUseCase {

    private final OrderConfirmService orderConfirmService;
    private final OrderAdaptor orderAdaptor;
    private final UserAdaptor userAdaptor;
    public OrderResponse execute(ConfirmOrderRequest confirmPaymentsRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Order order = orderConfirmService.execute(
            confirmPaymentsRequest.toConfirmPaymentsRequest(), currentUserId);

        return ;
    }
}
