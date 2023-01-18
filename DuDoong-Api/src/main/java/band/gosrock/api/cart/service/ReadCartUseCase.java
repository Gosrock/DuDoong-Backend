package band.gosrock.api.cart.service;


import band.gosrock.api.cart.model.dto.response.CartResponse;
import band.gosrock.api.cart.model.mapper.CartMapper;
import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadCartUseCase {
    private final CartMapper cartMapper;
    private final CartAdaptor cartAdaptor;

    /** 내가 지금 가지고 있는 장바구니를 리턴합니다. 에러 상황은 아니기때문에 없으면 null 리턴합니다. */
    @Transactional(readOnly = true)
    public CartResponse execute() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return cartAdaptor
                .findCartByUserId(currentUserId)
                .map(cartMapper::toCartResponse)
                .orElse(null);
    }
}
