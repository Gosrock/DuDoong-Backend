package band.gosrock.api.order.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.order.model.dto.request.CreateOrderRequest;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.service.CartToOrderService;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final CartToOrderService cartToOrderService;

    private final UserAdaptor userAdaptor;

    public CreateOrderResponse execute(CreateOrderRequest createOrderRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userAdaptor.queryUser(currentUserId);
        if (createOrderRequest.getCouponId() == null) {
            Order order =
                    cartToOrderService.creatOrderWithOutCoupon(
                            createOrderRequest.getCartId(), currentUserId);
            return CreateOrderResponse.from(order, user.getProfile());
        }
        return null;
    }
}
