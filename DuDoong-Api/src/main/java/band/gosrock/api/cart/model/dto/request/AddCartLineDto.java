package band.gosrock.api.cart.model.dto.request;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Min;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AddCartLineDto {
    @Schema(description = "주문할 아이템 아이디", defaultValue = "1")
    private final Long itemId;

    @Schema(description = "상품 수량", defaultValue = "1")
    @Min(1)
    private final Long quantity;

    @Schema(description = "상품 관련 옵션에 대한 답변")
    private List<AddCartOptionAnswerDto> options;

    @JsonIgnore
    public List<Long> getOptionIds() {
        return options.stream().map(AddCartOptionAnswerDto::getOptionId).toList();
    }

    //    public CartLineItem toCartLineItem(Long userId) {
    //        List<CartOptionAnswer> cartOptionAnswers =
    //                addCartOptionAnswerDtos.stream()
    //                        .map(AddCartOptionAnswerDto::toCartOptionAnswer)
    //                        .toList();
    //        return CartLineItem.builder()
    //                .itemId(itemId)
    //                .quantity(quantity)
    //                .cartOptionAnswers(cartOptionAnswers)
    //                .build();
    //    }
}
