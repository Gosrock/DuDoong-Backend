package band.gosrock.api.cart.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AddCartRequest {

    @Schema(description = "상품에 옵션이 있을시에 각기 답변마다 여러개를 보내주시면됩니다. 한번에 답변하기면 하나에 quantity 를 늘리면 됩니다.")
    @Size(min = 1)
    private List<AddCartLineDto> items;
}
