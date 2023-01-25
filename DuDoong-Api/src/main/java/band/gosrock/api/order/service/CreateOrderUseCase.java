package band.gosrock.api.order.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.order.model.dto.request.CreateOrderRequest;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.service.CreateOrderService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final CreateOrderService createOrderService;

    private final UserUtils userUtils;

    public CreateOrderResponse execute(CreateOrderRequest createOrderRequest) {
        User user = userUtils.getCurrentUser();
        Long couponId = createOrderRequest.getCouponId();
        Long cartId = createOrderRequest.getCartId();
        if (couponId == null) {
            return CreateOrderResponse.from(
                    createOrderService.WithOutCoupon(cartId, user.getId()), user.getProfile());
        }
        return CreateOrderResponse.from(
                createOrderService.withCoupon(cartId, user.getId(), couponId), user.getProfile());
    }
}
