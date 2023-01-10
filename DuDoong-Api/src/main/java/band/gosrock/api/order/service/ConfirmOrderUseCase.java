package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.model.dto.request.ConfirmOrderRequest;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.service.OrderConfirmService;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ConfirmOrderUseCase {

    private final OrderConfirmService orderConfirmService;
    private final OrderAdaptor orderAdaptor;
    public PaymentsResponse execute(ConfirmOrderRequest confirmPaymentsRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();



        return orderConfirmService.execute(confirmPaymentsRequest.toConfirmPaymentsRequest(),currentUserId);
    }
}
