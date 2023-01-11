package band.gosrock.api.cart.model.dto.response;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    // 일반티켓(1/3) - 4000원
    private String name;
    // 응답 목록
    private List<OptionAnswerVo> answers;

    private Money itemPrice;
    private Money cartLinePrice;

    public static CartItemResponse of(String name, CartLineItem cartLineItem) {
        return CartItemResponse.builder()
                .answers(cartLineItem.getOptionAnswerVos())
                .name(name)
                .cartLinePrice(cartLineItem.getTotalCartLinePrice())
                .itemPrice(cartLineItem.getItemPrice())
                .build();
    }
}
