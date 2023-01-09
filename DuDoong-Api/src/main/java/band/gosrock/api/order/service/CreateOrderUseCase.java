package band.gosrock.api.order.service;


import band.gosrock.api.order.model.dto.request.CreateOrderRequest;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.OrderDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderDomainService orderDomainService;

    public CreateOrderResponse execute(CreateOrderRequest createOrderRequest) {
        //        return orderDomainService.creatOrder();
        return null;
    }
}
