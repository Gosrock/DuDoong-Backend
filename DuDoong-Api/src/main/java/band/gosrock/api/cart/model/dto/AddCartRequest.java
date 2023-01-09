package band.gosrock.api.cart.model.dto;

import band.gosrock.domain.domains.cart.domain.CartLineItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AddCartRequest {

    @Schema(description = "상품에 옵션이 있을시에 각기 답변마다 여러개를 보내주시면됩니다. 한번에 답변하기면 하나에 quantity 를 늘리면 됩니다.")
    private final List<AddCartLineDto> addCartLineDtos;

    public List<CartLineItem> getCartLines(Long userId){
        return addCartLineDtos.stream().map(addCartLineDto -> addCartLineDto.toCartLineItem(userId)).toList();
    }
}
