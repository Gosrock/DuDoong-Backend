package band.gosrock.api.cart.service;


import band.gosrock.api.cart.model.dto.request.AddCartRequest;
import band.gosrock.api.cart.model.dto.response.CartResponse;
import band.gosrock.api.cart.model.mapper.CartMapper;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.cart.service.CartDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateCartUseCase {

    private final CartDomainService cartDomainService;
    private final CartMapper cartMapper;

    public CartResponse execute(AddCartRequest addCartRequest) {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        Cart cart = cartMapper.toEntity(addCartRequest, currentUserId);
        Long cartId = cartDomainService.createCart(cart);
        return cartMapper.toCartResponse(cartId);
    }
}
