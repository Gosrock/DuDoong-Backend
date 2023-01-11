package band.gosrock.api.cart.model.dto.response;


import band.gosrock.domain.domains.cart.domain.Cart;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCartResponse {

    private final String title;
    // 내티켓 확인하기
    private final List<CartItemResponse> items;

    // 금액
    private final String totalPrice;

    private final Long cartId;

    private final Long totalQuantity;

    public static CreateCartResponse of(List<CartItemResponse> cartItemResponses, Cart cart) {
        return CreateCartResponse.builder()
                .items(cartItemResponses)
                .totalPrice(cart.getTotalPrice().toString())
                .cartId(cart.getId())
                .title(cart.getCartName())
                .totalQuantity(cart.getTotalQuantity())
                .build();
    }
}
