package band.gosrock.api.cart.service;


import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CreateCartResponse;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import band.gosrock.domain.domains.cart.service.CartDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateCartLinesUseCase {

    private final CartDomainService cartDomainService;
    private final CartAdaptor cartAdaptor;

    public CreateCartResponse execute(AddCartRequest addCartRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<CartLineItem> cartLines = addCartRequest.getCartLines(currentUserId);

        Cart save =
                cartAdaptor.save(
                        Cart.builder().cartLineItems(cartLines).userId(currentUserId).build());

        //        CartItemOptionAnswerResponse cartItemOptionAnswerResponse =
        // CartItemOptionAnswerResponse.builder().answer()
        //            .question().build();
        //        CartItemResponse cartItemResponse =
        // CartItemResponse.builder().answers().title().build();
        //        CreateCartResponse createCartResponse =
        // CreateCartResponse.builder().cartItemDtos().amount().build();
        return null;
    }
}
