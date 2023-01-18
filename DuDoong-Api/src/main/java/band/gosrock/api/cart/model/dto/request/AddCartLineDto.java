package band.gosrock.api.cart.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Min;
import lombok.Getter;

@Getter
public class AddCartLineDto {
    @Schema(description = "주문할 아이템 아이디", defaultValue = "1")
    private Long itemId;

    @Schema(description = "상품 수량", defaultValue = "1")
    @Min(1)
    private Long quantity;

    @Schema(description = "상품 관련 옵션에 대한 답변")
    private List<AddCartOptionAnswerDto> options;
}
