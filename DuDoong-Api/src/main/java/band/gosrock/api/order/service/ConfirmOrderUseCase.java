package band.gosrock.api.order.service;


import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.OrderDomainService;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ConfirmOrderUseCase {

    private final OrderDomainService orderDomainService;
    public PaymentsResponse execute(ConfirmPaymentsRequest confirmPaymentsRequest) {
        return orderDomainService.confirmOrder(confirmPaymentsRequest);
    }
}
