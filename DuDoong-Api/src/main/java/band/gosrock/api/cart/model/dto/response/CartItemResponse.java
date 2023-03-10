package band.gosrock.api.cart.model.dto.response;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.cart.domain.CartLineItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartItemResponse {

    // 일반 티켓 2매 이름
    @Schema(description = "카트라인의 이름입니다.", defaultValue = "일반티켓 2매")
    private String name;
    // 응답 목록
    private List<OptionAnswerVo> answers;

    @Schema(description = "아이템 공급가액입니다.", defaultValue = "3000원")
    private Money itemPrice;

    @Schema(
            description = "카트 라인의 총 가격입니다. 옵션등을 통해서 공급가액에 합산되는 형식입니다. (아이템가격 + 옵션가) * 아이템 개수",
            defaultValue = "4000원")
    private Money cartLinePrice;

    @Schema(description = "담은 상품의 개수입니다.", defaultValue = "1")
    private Long packedQuantity;

    @Schema(description = "각 옵션 가격")
    private final Money eachOptionPrice;

    public static CartItemResponse of(
            CartLineItem cartLineItem, String itemName, List<OptionAnswerVo> answers) {
        return CartItemResponse.builder()
                .answers(answers)
                .name(itemName)
                .cartLinePrice(cartLineItem.getTotalCartLinePrice())
                .itemPrice(cartLineItem.getItemPrice())
                .packedQuantity(cartLineItem.getQuantity())
                .eachOptionPrice(cartLineItem.getTotalOptionsPrice())
                .build();
    }
}
